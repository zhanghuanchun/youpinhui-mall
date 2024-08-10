package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.user.LoginVo;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author zhc
 * @Create 2024/7/30 16:04
 */
@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public UserInfo login(LoginVo loginVo) {
        //  构建查询条件
        LambdaQueryWrapper<UserInfo> userInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //  优化：select * from user_info where (login_name = ? or email = ? or phone_num = ?) and passwd = ?;
        userInfoLambdaQueryWrapper.and(query -> {
            query.eq(UserInfo::getLoginName, loginVo.getLoginName())
                    .or().eq(UserInfo::getEmail, loginVo.getLoginName())
                    .or().eq(UserInfo::getPhoneNum, loginVo.getLoginName());
        });

        //  这个不能这么使用！ 因为密码是暗文！
        //  获取用户输入的密码
        String newPwd = MD5.encrypt(loginVo.getPasswd());
        //  String newPwd = DigestUtils.md5DigestAsHex(loginVo.getPasswd().getBytes());
        userInfoLambdaQueryWrapper.eq(UserInfo::getPasswd,newPwd);
        UserInfo userInfo = userInfoMapper.selectOne(userInfoLambdaQueryWrapper);
        //返回登录对象
        return userInfo;
    }
}
