package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.ItemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.thymeleaf.util.PatternUtils;

import java.util.Map;

/**
 * @Author zhc
 * @Create 2024/7/26 19:17
 */
@Controller
public class ItemController {

    @Autowired
    private ItemFeignClient itemFeignClient;
    //
    @GetMapping("{skuId}.html")
    public String item(@PathVariable Long skuId, Model model){
        Result<Map> itemAllData = itemFeignClient.getItem(skuId);
        model.addAllAttributes(itemAllData.getData());
        return "item/item";
    }



}
