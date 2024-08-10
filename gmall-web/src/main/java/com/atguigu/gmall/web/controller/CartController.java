package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.client.ProductFeignClient;
import com.atguigu.gmall.model.product.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 购物车页面
 * </p>
 */
@Controller
public class CartController {

    @Autowired
    private ProductFeignClient productFeignClient;

    @GetMapping("addCart.html")
    public String addToCart(HttpServletRequest request) {

        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");
        SkuInfo skuInfo = productFeignClient.getSkuInfo(Long.valueOf(skuId));
        request.setAttribute("skuInfo", skuInfo);
        request.setAttribute("skuNum", skuNum);
        return "cart/addCart";
    }

    @GetMapping("cart.html")
    public String cartList() {
        return "cart/index";
    }

}