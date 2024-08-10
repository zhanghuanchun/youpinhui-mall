package com.atguigu.gmall.cart.impl;

import com.atguigu.gmall.cart.CartFeignClient;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author zhc
 * @Create 2024/8/1 17:25
 */
@Component
public class CartDegradeFeignClient implements CartFeignClient {
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        System.out.println("getCartCheckedList 远程接口调用失败。。。");
        return null;
    }
}
