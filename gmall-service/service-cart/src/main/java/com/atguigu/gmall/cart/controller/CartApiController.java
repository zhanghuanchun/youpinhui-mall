package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @Author zhc
 * @Create 2024/7/31 22:45
 */
@Api(tags = "购物车后台管理")
@RestController
@RequestMapping("/api/cart")
public class CartApiController {
    @Autowired
    private CartService cartService;


    /**
     * 添加购物车
     *
     * @param skuId
     * @param skuNum
     * @param request
     * @return
     */
    @GetMapping("/addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum,
                            HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.addToCart(skuId, skuNum, userId);

        return Result.ok();
    }

    /**
     * 查询用户购物车列表
     * 版本1：分别查询未登录购物车列表，以及登录的购物车列表
     * 版本2：将两个购物车中商品合并
     *
     * @param request
     * @return
     */
    @GetMapping("/cartList")
    public Result<List<CartInfo>> cartList(HttpServletRequest request) {

        String userId = AuthContextHolder.getUserId(request);
        String userTempId = AuthContextHolder.getUserTempId(request);
        List<CartInfo> cartInfoList = this.cartService.getCartList(userId, userTempId);
        return Result.ok(cartInfoList);
    }

    //  选中状态

    /**
     * 修改购物车的选中状态
     *
     * @param skuId
     * @param isChecked
     * @param request
     * @return
     */
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request) {
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        this.cartService.checkCart(skuId, isChecked, userId);
        return Result.ok();
    }

    /**
     * 全选修改状态
     *
     * @param isChecked
     * @param request
     * @return
     */
    @GetMapping("/allCheckCart/{isChecked}")
    public Result allCheckCart(@PathVariable Integer isChecked, HttpServletRequest request) {
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        this.cartService.allCheckCart(userId, isChecked);
        return Result.ok();
    }
    /**
     * 删除
     *
     * @param skuId
     * @param request
     * @return
     */
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable("skuId") Long skuId,
                             HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        this.cartService.deleteCart(userId, skuId);
        return Result.ok();
    }

    /**
     * 清空购物车.
     * @return
     */
    @GetMapping("clearCart")
    public Result clearCart(HttpServletRequest request){
        //  获取登录用户Id ---> 存储到请求头中!
        String userId = AuthContextHolder.getUserId(request);
        //  需要临时用户Id ，页面js 中，判断如果未登录，就会生成临时用户Id存储到cookie！
        if (StringUtils.isEmpty(userId)){
            //  从请求头中获取临时用户Id
            userId = AuthContextHolder.getUserTempId(request);
        }
        //  调用服务层方法.
        this.cartService.clearCart(userId);
        return Result.ok();
    }

    /**
     * 查询用户购物车中已勾选的商品列表
     * @param userId
     * @return
     */
    @GetMapping("/getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable("userId") String userId){
        List<CartInfo> list = cartService.getCartCheckedList(userId);
        return list;
    }
}
