package com.atguigu.gmall.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.LoginVo;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserInfoService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author zhc
 * @Create 2024/7/30 15:58
 */
@Api(tags = "用户登录")
@RestController
@RequestMapping("/api/user/passport")
public class PassportApiController {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserInfoService userInfoService;

    @PostMapping("/login")
    public Result login(@RequestBody LoginVo loginVo, HttpServletRequest request) {
        //  调用服务层方法。
        UserInfo userInfo = userInfoService.login(loginVo);
        if (userInfo != null) {



            //  创建一个map 集合
            Map<String, Object> map = new HashMap<>();
            //  通过前端页面：需要一个token 数据; 前端后续会使用 token 判断这个用户是否登录.
            String token = UUID.randomUUID().toString();
            map.put("token", token);

            //  存储用户昵称
            map.put("nickName", userInfo.getNickName());

            //  将用户信息存储到redis 中.  user:login:userId==不行！ user:login:token == 可以！
            String loginKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            //  声明一个对象
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", userInfo.getId().toString());
            //  防止token 被盗用！ ip 地址是谁的? 服务器的IP地址 ; 一台服务器就废了！
            //  在登录成功的时候，写入20个key u1 u2 u3.。。 u20 ; 返回之前判断ip+20个key;
            //map.put("u1",u1);map.put("u2",u2);map.put("u3",u3);...map.put("u20",u20);
            jsonObject.put("ip", IpUtil.getIpAddress(request));
            //  数据类型 String;
            //  token 是存储在cookie 中的！ cookie 不安全，可以修改！ 被盗用！
            redisTemplate.opsForValue().set(loginKey, jsonObject.toString(), RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            //  返回给前端
            return Result.ok(map);
        } else {
            return Result.fail().message("登录失败.");
        }
    }

    /**
     * 退出登录
     *
     * @param request
     * @return
     */
    @GetMapping("logout")
    public Result logout(@RequestHeader String token,
                         HttpServletRequest request) {
        // 获取token信息
        String token1 = request.getHeader("token");
        //获取缓存的key
        String loginKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
        //删除数据
        redisTemplate.delete(loginKey);
        //本质删除cookie 数据 与缓存数据
        return Result.ok();
    }

}
