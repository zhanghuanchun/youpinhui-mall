package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author: atguigu
 * @create: 2023-01-28 16:19
 */
@Slf4j
@RestController
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${app_id}")
    private String app_id;

    /**
     * 生成二维码
     *
     * @param orderId 订单ID
     * @return
     */
    @GetMapping("/submit/{orderId}")
    public String createAlipayForm(@PathVariable("orderId") Long orderId) {
        String form = "";
        try {
            form = alipayService.createAlipay(orderId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return form;
    }

    /**
     * 处理支付宝支付页面同步回调-给用户展示支付成功页面
     */
    @GetMapping("/callback/return")
    public void returnPaySuccessPage(@RequestParam Map<String, String> map, HttpServletResponse response) throws IOException {
        //可以修改交易记录和订单状态
        log.info("同步回调参数：{}" + map);
        //重定向到支付成功页面
        response.sendRedirect(AlipayConfig.return_order_url);
    }

    /**
     * 处理用户支付成功后-支付宝提交异步回调
     *
     * @param paramsMap 获取异步通知参数
     * @return
     */
    @PostMapping("/callback/notify")
    public String payResultNotify(@RequestParam Map<String, String> paramsMap) {
        log.info("==========================异步回调参数：{}" + paramsMap);
        System.out.println("==========================异步回调参数：{}" + paramsMap);
        //调用SDK验证签名
        boolean signVerified = false;
        try {
            signVerified = AlipaySignature.rsaCheckV1(
                    paramsMap,
                    AlipayConfig.alipay_public_key,
                    AlipayConfig.charset,
                    AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        if (signVerified) {
            // 1 2 4 系统校验
            // 1 获取支付传递过来的 out_trade_no 判断它是否与商家系统的订单号一致！
            String outTradeNo = paramsMap.get("out_trade_no");
            PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
            if (paymentInfo == null) {
                return "failure";
            }
            // 2 获取订单总金额 ，校验总金额，是否为该订单的实际金额（即是商家订单创建时的金额）
            String totalAmount = paramsMap.get("total_amount");
            if (new BigDecimal("0.01").compareTo(new BigDecimal(totalAmount)) != 0) {
                return "failure";
            }
            // 4 判断app 是否是商家本身
            String appId = paramsMap.get("app_id");
            if (!appId.equals(this.app_id)) {
                return "failure";
            }
            //保证消息的幂等性
            //过滤重复通知结果数据 支付宝针对同一条异步通知重试时 异步通知参数中的 notify_id 是不变的
            String notifyId = paramsMap.get("notify_id");
            try {
                Boolean result = redisTemplate.opsForValue().setIfAbsent(notifyId, notifyId, 1462, TimeUnit.MINUTES);
                if (!result) {
                    // 停止后续业务处理！
                    return "failure";
                }
                // 获取交易状态
                String tradeStatus = paramsMap.get("trade_status");
                // 判断交易状态
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    //修改交易记录状态  payment_status ; callback_time ; callback_content ; trade_no ;
                    paymentService.updatePaymentInfoStatus(outTradeNo, PaymentType.ALIPAY.name(), paramsMap);
                    return "success";
                }
            } catch (Exception e) {
                //删除缓存的key
                this.redisTemplate.delete(notifyId);
                log.error("处理异步通知失败 {}" + outTradeNo);
                throw new RuntimeException(e);
            }
        } else {
            // 停止后续业务处理！
            return "failure";
        }
        return "failure";
    }

    /**
     * 发起退款
     *
     * @param orderId
     * @return
     */
    @GetMapping("refund/{orderId}")
    public Result refund(@PathVariable(value = "orderId") Long orderId) {
        // 调用退款接口
        boolean flag = alipayService.refund(orderId);
        return Result.ok(flag);
    }

    /**
     * 根据订单ID关闭支付宝交易记录
     *
     * @param orderId 订单ID
     * @return
     */
    @GetMapping("closePay/{orderId}")
    public Boolean closePay(@PathVariable Long orderId) {
        return alipayService.closePay(orderId);
    }

    /**
     * 根据订单id 查看支付宝交易记录
     *
     * @param orderId
     * @return
     */
    @GetMapping("checkPayment/{orderId}")
    public Boolean checkPayment(@PathVariable Long orderId) {
        //调用服务层关闭的方法
        Boolean flag = this.alipayService.checkPayment(orderId);
        //返回结果
        return flag;
    }

    /**
     * 查询是否本地交易记录 \
     * @param outTradeNo
     * @return
     */
    @GetMapping("/getPaymentInfo/{outTradeNo}")
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo) {
        return this.paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
    }

}