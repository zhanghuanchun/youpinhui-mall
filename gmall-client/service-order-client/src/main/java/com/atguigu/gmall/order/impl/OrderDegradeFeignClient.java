package com.atguigu.gmall.order.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.OrderFeignClient;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @Author zhc
 * @Create 2024/8/1 18:28
 */
@Component
public class OrderDegradeFeignClient implements OrderFeignClient {
    @Override
    public Result<Map<String, Object>> trade() {
        System.out.println("trade 远程调用接口失败。。。");
        return null;
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        System.out.println("getOrderInfo 远程调用接口失败。。。");
        return null;
    }

    @Override
    public Long submitSeckillOrder(OrderInfo orderInfo) {
        System.out.println("submitSeckillOrder 远程调用接口失败。。。");
        return null;
    }
}
