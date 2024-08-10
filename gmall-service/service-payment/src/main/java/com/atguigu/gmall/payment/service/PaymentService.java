package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

/**
 * @Author zhc
 * @Create 2024/8/3 22:36
 */
public interface PaymentService {
    /**
     * 保存交易记录
     * @param orderInfo
     * @param paymentType 支付类型（1：微信 2：支付宝）
     */
    void savePaymentInfo(OrderInfo orderInfo, String paymentType);

    /**
     * 根据第三方交易编号，查询 PaymentInfo
     * @param outTradeNo
     * @param name
     * @return
     */
    PaymentInfo getPaymentInfo(String outTradeNo, String name);

    /**
     * 更新交易状态
     * @param outTradeNo
     * @param name
     * @param paramsMap
     */
    void updatePaymentInfoStatus(String outTradeNo, String name, Map<String, String> paramsMap);

    /**
     * 更新交易记录状态
     * @param outTradeNo
     * @param name
     * @param paymentInfo
     */
    void updatePaymentInfoStatus(String outTradeNo, String name, PaymentInfo paymentInfo);

    /**
     * 根据交易主键去修改交易记录
     * @param id
     */
    void closePaymentInfo(Long id);
}
