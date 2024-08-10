package com.atguigu.gmall.list.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.stereotype.Component;

@Component
public class ListDegradeFeignClient implements ListFeignClient {

    @Override
    public Result incrHotScore(Long skuId) {
        System.out.println("incrHotScore 远程接口调用失败。。。");
        return null;
    }

    @Override
    public Result list(SearchParam searchParam) {
        System.out.println("list 远程接口调用失败。。。");
        return null;
    }

    @Override
    public Result upperGoods(Long skuId) {
        System.out.println("upperGoods 远程接口调用失败。。。");
        return null;
    }

    @Override
    public Result lowerGoods(Long skuId) {
        System.out.println("lowerGoods 远程接口调用失败。。。");
        return null;
    }
}