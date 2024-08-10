package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * @Author zhc
 * @Create 2024/8/3 22:49
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        // 细节处理
        LambdaQueryWrapper<PaymentInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaymentInfo::getOrderId, orderInfo.getId());
        queryWrapper.eq(PaymentInfo::getPaymentType, paymentType);
        PaymentInfo paymentInfoQuery = paymentInfoMapper.selectOne(queryWrapper);
        //判断交易记录表中是否又这条数据
        if (paymentInfoQuery != null) return;

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setUserId(orderInfo.getUserId());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setUpdateTime(new Date());
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setSubject(orderInfo.getTradeBody());

        paymentInfoMapper.insert(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String name) {
        LambdaQueryWrapper<PaymentInfo> paymentInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        paymentInfoLambdaQueryWrapper.eq(PaymentInfo::getOutTradeNo, outTradeNo).
                eq(PaymentInfo::getPaymentType, name);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoLambdaQueryWrapper);
        return paymentInfo;
    }

    @Override
    public void updatePaymentInfoStatus(String outTradeNo, String name, Map<String, String> paramsMap) {
        //创建更新/查询条件
        LambdaUpdateWrapper<PaymentInfo> paymentInfoLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        paymentInfoLambdaUpdateWrapper.
                eq(PaymentInfo::getOutTradeNo, outTradeNo).
                eq(PaymentInfo::getPaymentType, name);
        //查询paymentInfo
        PaymentInfo paymentInfoQuery = paymentInfoMapper.selectOne(paymentInfoLambdaUpdateWrapper);
        if (paymentInfoQuery == null) {
            return;
        }
        // 修改交易记录状态  payment_status ; callback_time ; callback_content ; trade_no ;
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(paramsMap.toString());
        paymentInfo.setTradeNo(paramsMap.get("trade_no"));
        paymentInfoMapper.update(paymentInfo, paymentInfoLambdaUpdateWrapper);

        //发送消息更改订单的状态
        rabbitService.sendMessage(
                MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,
                MqConst.ROUTING_PAYMENT_PAY,
                paymentInfoQuery.getOrderId()
        );
    }

    @Override
    public void updatePaymentInfoStatus(String outTradeNo, String name, PaymentInfo paymentInfo) {
        //创建更新条件
        LambdaUpdateWrapper<PaymentInfo> paymentInfoLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        paymentInfoLambdaUpdateWrapper.
                eq(PaymentInfo::getOutTradeNo, outTradeNo).
                eq(PaymentInfo::getPaymentType, name);
        // 调用更新方法
        paymentInfoMapper.update(paymentInfo, paymentInfoLambdaUpdateWrapper);
    }

    @Override
    public void closePaymentInfo(Long id) {
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setId(id);
        paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
        //调用mapper 去关闭交易记录
        paymentInfoMapper.updateById(paymentInfo);

    }
}
