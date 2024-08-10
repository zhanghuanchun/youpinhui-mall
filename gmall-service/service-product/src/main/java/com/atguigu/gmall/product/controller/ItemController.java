package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.model.item.Item;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test GlobalExceptionHandler Controller
 */
@RestController
@RequestMapping("admin")
public class ItemController {

    @PostMapping("item")
    public Result<Item> saveItem(Item item) {
        // 模拟业务执行出现异常
        int i = 1/0;
        // 保存业务
        System.out.println("保存成功");
        return Result.build(item, ResultCodeEnum.SUCCESS);
    }
}