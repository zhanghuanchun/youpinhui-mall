package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Api(tags = "商品详情内部数据接口")
@RestController
@RequestMapping("/api/item")
public class ItemApiController {

    @Autowired
    private ItemService itemService;

    @GetMapping("/{skuId}")
    public Result getItem(@PathVariable Long skuId){
        //  调用服务层方法. 返回值是什么，如何确定！
        //  这里需要提供数据给 web-all 使用，并且将数据展示出去！
        Map map = itemService.getItem(skuId);
        return Result.ok(map);
    }
}