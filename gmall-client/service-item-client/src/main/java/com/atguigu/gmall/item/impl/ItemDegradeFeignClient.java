package com.atguigu.gmall.item.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.ItemFeignClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Author zhc
 * @Create 2024/7/26 18:53
 */
@Component
public class ItemDegradeFeignClient implements ItemFeignClient {
    @Override
    public Result<Map> getItem(Long skuId) {
        System.out.println("getItem调用失败。。。");
        return null;
    }
}
