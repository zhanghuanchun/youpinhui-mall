package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.service.UserAddressService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @Author zhc
 * @Create 2024/8/1 16:52
 */
@Api(tags = "获取用户地址")
@RestController
@RequestMapping("/api/user/userAddress")
public class UserApiController {

    //  注入服务层
    @Autowired
    private UserAddressService userAddressService;

    //  移动端获取收货地址列表.
    //  http://localhost/api/user/userAddress/auth/findUserAddressList

    /**
     * 根据userid 获取地址列表
     * @param request
     * @return
     */
    @GetMapping("/auth/findUserAddressList")
    public Result findUserAddressList(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //  需要获取到收货地址列表.
        //  调用服务层方法
        List<UserAddress> userAddressList = userAddressService.getUserAddressListByUserId(userId);
        return Result.ok(userAddressList);
    }

}
