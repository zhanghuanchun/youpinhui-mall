package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.LoginVo;
import com.atguigu.gmall.model.user.UserInfo;

import java.util.Map;

/**
 * @Author zhc
 * @Create 2024/7/30 16:04
 */
public interface UserInfoService {
    UserInfo  login(LoginVo loginVo);
}
