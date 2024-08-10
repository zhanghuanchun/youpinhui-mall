package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;

import java.util.List;

/**
 * @Author zhc
 * @Create 2024/8/1 16:58
 */
public interface UserAddressService {
    List<UserAddress> getUserAddressListByUserId(String userId);


}
