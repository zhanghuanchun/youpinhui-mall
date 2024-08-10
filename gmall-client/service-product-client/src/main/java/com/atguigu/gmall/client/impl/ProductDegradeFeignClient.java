package com.atguigu.gmall.client.impl;

import com.atguigu.gmall.client.ProductFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @Author zhc
 * @Create 2024/7/25 14:40
 */
@Component
public class ProductDegradeFeignClient implements ProductFeignClient {
    @Override
    public SkuInfo getSkuInfo(Long skuId) {
        //如果远程调用getSkuInfo失败
        System.out.println("getSkuInfo远程调用失败。。。");
        return null;
    }

    @Override
    public BaseCategoryView getCategoryView(Long category3Id) {
        System.out.println("getCategoryView远程调用失败。。。");
        return null;
    }

    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        System.out.println("getSkuPrice远程调用失败。。。");
        return null;
    }

    @Override
    public List<SpuPoster> getSpuPosterBySpuId(Long spuId) {
        System.out.println("getSpuPosterBySpuId远程调用失败。。。");
        return null;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        System.out.println("getAttrList远程调用失败。。。");
        return null;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        System.out.println("getSpuSaleAttrListCheckBySku远程调用失败。。。");
        return null;
    }

    @Override
    public Map getSkuValueIdsMap(Long spuId) {
        System.out.println("getSkuValueIdsMap远程调用失败。。。");
        return null;
    }

    @Override
    public Result getBaseCategoryList() {
        System.out.println("getBaseCategoryList 远程调用失败。。。");
        return null;
    }

    @Override
    public BaseTrademark getTrademarkById(Long tmId) {
        System.out.println("getTrademarkById 远程调用失败。。。");
        return null;
    }
}
