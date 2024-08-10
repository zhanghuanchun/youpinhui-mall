package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

/**
 * @Author zhc
 * @Create 2024/7/31 23:03
 */
public interface CartService {
    /**
     * 添加购物车
     * @param skuId
     * @param skuNum
     * @return
     */
    void addToCart(Long skuId, Integer skuNum, String userId);

    /**
     * 查看购物车列表
     * @param userId
     * @param userTempId
     * @return
     */
    List<CartInfo> getCartList(String userId, String userTempId);

    void checkCart(Long skuId, Integer isChecked, String userId);

    void allCheckCart(String userId, Integer isChecked);

    void deleteCart(String userId, Long skuId);

    void clearCart(String userId);

    List<CartInfo> getCartCheckedList(String userId);
}
