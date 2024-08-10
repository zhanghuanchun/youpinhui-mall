package com.atguigu.gmall.activity.client.impl;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @Author zhc
 * @Create 2024/8/7 21:59
 */
@Component
public class ActivityDegradeFeignClient implements ActivityFeignClient {
    @Override
    public Result<List<SeckillGoods>> findAll() {
        System.out.println(" findAll 远程接口调用失败。。。");
        return null;
    }

    @Override
    public Result<SeckillGoods> getSeckillGoods(Long skuId) {
        System.out.println(" getSeckillGoods 远程接口调用失败。。。");
        return null;
    }

    @Override
    public Result<Map<String, Object>> seckillTradeData() {
        System.out.println(" seckillTradeData 远程接口调用失败。。。");
        return null;
    }
}
