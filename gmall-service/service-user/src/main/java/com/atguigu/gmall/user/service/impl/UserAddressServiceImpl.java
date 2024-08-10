package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.service.UserAddressService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author zhc
 * @Create 2024/8/1 16:59
 */
@Service
public class UserAddressServiceImpl implements UserAddressService {

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Override
    public List<UserAddress> getUserAddressListByUserId(String userId) {
        //  select * from user_address where user_id = ?;
        LambdaQueryWrapper<UserAddress> userAddressLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userAddressLambdaQueryWrapper.eq(UserAddress::getUserId,userId);
        return userAddressMapper.selectList(userAddressLambdaQueryWrapper);
    }
}
