package com.atguigu.gmall.activity.client;

import com.atguigu.gmall.activity.client.impl.ActivityDegradeFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(value = "service-activity", fallback = ActivityDegradeFeignClient.class)
public interface ActivityFeignClient {

    /**
     * 返回全部列表
     *
     * @return
     */
    @GetMapping("/api/activity/seckill/findAll")
    Result<List<SeckillGoods>> findAll();

    /**
     * 获取实体
     *
     * @param skuId
     * @return
     */
    @GetMapping("/api/activity/seckill/getSeckillGoods/{skuId}")
    Result<SeckillGoods> getSeckillGoods(@PathVariable("skuId") Long skuId);

    /**
     * 秒杀结算页面
     * @return
     */
    @GetMapping("/api/activity/seckill/auth/trade")
    Result<Map<String, Object>> seckillTradeData();
}