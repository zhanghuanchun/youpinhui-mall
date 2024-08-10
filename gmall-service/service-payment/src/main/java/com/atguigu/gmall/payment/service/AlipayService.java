package com.atguigu.gmall.payment.service;

/**
 * @Author zhc
 * @Create 2024/8/3 23:33
 */
public interface AlipayService {
    String createAlipay(Long orderId);

    boolean refund(Long orderId);
    /**
     * 根据订单ID关闭支付宝交易记录
     * @param orderId 订单ID
     * @return
     */
    Boolean closePay(Long orderId);

    Boolean checkPayment(Long orderId);

}
