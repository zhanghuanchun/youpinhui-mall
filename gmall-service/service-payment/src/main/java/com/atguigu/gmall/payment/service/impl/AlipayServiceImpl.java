package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.OrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @Author zhc
 * @Create 2024/8/3 23:33
 */
@Slf4j
@Service
public class AlipayServiceImpl implements AlipayService {
    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public String createAlipay(Long orderId) {
        try {
            //1.远程调用订单微服务获取订单信息 判断订单状态
            OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
            //2.保存本地交易记录
            paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());

            if (orderId == null || OrderStatus.PAID.name().equals(orderInfo.getOrderStatus())
                    || OrderStatus.CLOSED.name().equals(orderInfo.getOrderStatus())) {
                return "当前订单已支付，或已关闭";
            }
            //3.调用支付宝-统一收单下单并支付页面接口
            //3.1 创建支付宝页面请求对象
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            //3.2 设置相关请求参数
            //支付宝异步回调支付微服务通知支付结果--通知商户系统
            request.setNotifyUrl(AlipayConfig.notify_payment_url);
            //支付宝同步支付结果结果回调-通知用户
            request.setReturnUrl(AlipayConfig.return_payment_url);
            JSONObject bizContent = new JSONObject();
            //商家自定义订单编号
            bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
            //订单金额
            bizContent.put("total_amount", "0.01");
            //商品描述
            bizContent.put("subject", orderInfo.getTradeBody());
            //支付宝端支付的有效期
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 10);
            bizContent.put("time_expire", simpleDateFormat.format(calendar.getTime()));
//            bizContent.put("timeout_express", "10m");
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
            request.setBizContent(bizContent.toString());
//            log.info("createAlipay 创建二维码接口 request : {}" + request);
            //3.3调用支付宝生成页面接口
            String form = alipayClient.pageExecute(request).getBody();
            return form;
        } catch (AlipayApiException e) {
            log.error("生成支付二维码失败：{}" + orderId + "失败原因：{}" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean refund(Long orderId) {
        // 需要使用商户订单编号
        OrderInfo orderInfo = this.orderFeignClient.getOrderInfo(orderId);
        //如果当前订单已关闭 无退款
        if (orderInfo.getOrderStatus().equals("CLOSED")) {
            return false;
        }
        //  发起的退款请求对象.
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        //  bizContent.put("trade_no", "2021081722001419121412730660");
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        bizContent.put("refund_amount", 0.01);
//        bizContent.put("out_request_no", "HZ01RF001");

        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        if (response.isSuccess()) {
            //  退款成功.  修改交易记录状态. 订单状态.
            if ("Y".equals(response.getFundChange())) {
                System.out.println("调用成功");
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
                paymentInfo.setTradeNo(response.getTradeNo());
                //  调用更新方法.方法重载
                this.paymentService.updatePaymentInfoStatus(orderInfo.getOutTradeNo(), PaymentType.ALIPAY.name(), paymentInfo);
                //  异步：发送消息
                this.rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER_CLOSED, MqConst.ROUTING_ORDER_CLOSED, orderId);

                return true;
            } else {
                return false;
            }
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    /**
     * 根据订单ID关闭支付宝交易记录
     *
     * @param orderId 订单ID
     * @return
     */
    @Override
    public Boolean closePay(Long orderId) {
        try {
            //1.获取订单对象
            OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
            //2.调用支付宝接口关闭支付宝交易
            if (orderInfo != null) {
                AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
                JSONObject bizContent = new JSONObject();
//                bizContent.put("trade_no", orderInfo.getTradeNo());
                bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
                request.setBizContent(bizContent.toString());
                AlipayTradeCloseResponse response = alipayClient.execute(request);
                if (response.isSuccess()) {
                    // 表示关闭成功
                    return true;
                } else {
                    // 表示关闭失败
                    return false;
                }
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Boolean checkPayment(Long orderId) {
        try {
            //1.获取订单对象
            OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);

            //2.调用支付宝接口完成检查
            if (orderInfo != null) {
                AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
                JSONObject bizContent = new JSONObject();
                bizContent.put("out_trade_no", orderInfo.getOutTradeNo());

                request.setBizContent(bizContent.toString());

                AlipayTradeQueryResponse response = alipayClient.execute(request);
                if (response.isSuccess()) { //有交易记录
                    //如果你想判断是否支付成功，需要添加这个业务逻辑
//                    String tradeStatus = response.getTradeStatus();
//                    if ("WAIT_BUYER_PAY".equals(tradeStatus)) {
//                        log.info("等待付款");
//                    } else if ("TRADE_SUCCESS".equals(tradeStatus)) {
//                        log.info("支付成功");
//                    } else {
//                        log.info("扫码未支付，超时关闭");
//                    }

                    return true;
                } else {
                    //没有交易记录
                    return false;
                }
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return false;
    }
}
