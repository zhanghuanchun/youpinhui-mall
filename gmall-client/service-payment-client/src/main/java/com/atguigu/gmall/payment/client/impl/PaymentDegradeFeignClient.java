package com.atguigu.gmall.payment.client.impl;

import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import org.springframework.stereotype.Component;

/**
 * @Author zhc
 * @Create 2024/8/6 15:44
 */
@Component
public class PaymentDegradeFeignClient implements PaymentFeignClient {
    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo) {
        System.out.println(" getPaymentInfo 远程接口调用失败。。。");
        return null;
    }

    @Override
    public Boolean closePay(Long orderId) {
        System.out.println(" closePay 远程接口调用失败。。。");

        return null;
    }

    @Override
    public Boolean checkPayment(Long orderId) {
        System.out.println(" checkPayment 远程接口调用失败。。。");
        return null;
    }
}
