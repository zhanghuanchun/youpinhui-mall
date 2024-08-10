package com.atguigu.gmall.item;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.impl.ItemDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(value = "service-item", fallback = ItemDegradeFeignClient.class)
public interface ItemFeignClient {

    /**
     * 根据skuId 调用商品详情
     * @param skuId
     * @return
     */
    @GetMapping("/api/item/{skuId}")
    Result<Map> getItem(@PathVariable("skuId") Long skuId);
}