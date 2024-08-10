package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.client.ProductFeignClient;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.item.config.ThreadPoolConfig;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.*;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @Author zhc
 * @Create 2024/7/25 14:58
 */
@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ListFeignClient listFeignClient;

    /**
     * 多线程优化
     * @param skuId
     * @return
     */
    @Override
    public Map getItem(Long skuId) {
        //创建map 集合来存储商品详情页面需要的数据
        Map map = new HashMap();
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        if (!bloomFilter.contains(skuId)) {
            //没有就返回空对象
            return map;
        }
        //远程调用 获取skuInfo 数据
        // 使用supplyAsync 有返回值 --> 后续远程调用需要使用skuInfo 对象中的其他属性数据
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            map.put("skuInfo",skuInfo);
            return skuInfo;
        });
        // 查询分类数据
        // thenApply 有返回值 return ;thenAccept 没有返回值 直接存储map就行
        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            map.put("categoryView", categoryView);
        },threadPoolExecutor);

        // 查询最新价格
        CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal price = productFeignClient.getSkuPrice(skuId);
            map.put("price", price);
        },threadPoolExecutor);

        //查询海报信息
        CompletableFuture<Void> spuPosterBySpuIdCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            List<SpuPoster> spuPosterList = productFeignClient.getSpuPosterBySpuId(skuInfo.getSpuId());
            map.put("spuPosterList", spuPosterList);
        },threadPoolExecutor);

        //根据skuID 获取平台属性数据
        CompletableFuture<Void> attrListCompletableFuture = CompletableFuture.runAsync(() -> {
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
            List<Map<String, String>> mapList = attrList.stream().map(baseAttrInfo -> {
                Map<String, String> c_map = new HashMap<>();
                c_map.put("attrName", baseAttrInfo.getAttrName());
                c_map.put("attrValue", baseAttrInfo.getAttrValueList().get(0).getValueName());
                return c_map;
            }).collect(Collectors.toList());
            map.put("skuAttrList", mapList);
        },threadPoolExecutor);

        //获取销售属性
        CompletableFuture<Void> spuSaleAttrListCheckBySkuCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            map.put("spuSaleAttrList", spuSaleAttrList);
        },threadPoolExecutor);
        //获取切换功能数据

        CompletableFuture<Void> skuValueIdsMapCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            String jsonStr = JSON.toJSONString(skuValueIdsMap);
            map.put("valuesSkuJson", jsonStr);
        },threadPoolExecutor);
        CompletableFuture<Void> hotScoreCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        },threadPoolExecutor);
        CompletableFuture.allOf(
                skuInfoCompletableFuture,
                categoryViewCompletableFuture,
                skuPriceCompletableFuture,
                spuPosterBySpuIdCompletableFuture,
                attrListCompletableFuture,
                spuSaleAttrListCheckBySkuCompletableFuture,
                skuValueIdsMapCompletableFuture,
                hotScoreCompletableFuture
        ).join();
        //存储数据
        return map;
    }
    /*
    @Override
    public Map getItem(Long skuId) {
        //创建map 集合来存储商品详情页面需要的数据
        Map map = new HashMap();
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        if (!bloomFilter.contains(skuId)) {
            //没有就返回空对象
            return map;
        }
        //远程调用 获取skuInfo 数据
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        // 查询分类数据
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        // 查询最新价格
        BigDecimal price = productFeignClient.getSkuPrice(skuId);
        //查询海报信息
        List<SpuPoster> spuPosterList = productFeignClient.getSpuPosterBySpuId(skuInfo.getSpuId());
        //根据skuID 获取平台属性数据
        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
//        List<Map> mapList = new ArrayList<>();
//        if(!CollectionUtils.isEmpty(attrList)){
//            attrList.forEach(baseAttrInfo -> {
//                Map<String,String> c_map = new HashMap<>();
//                c_map.put("attrName",baseAttrInfo.getAttrName());
//                c_map.put("attrValue",baseAttrInfo.getAttrValueList().get(0).getValueName());
//                mapList.add(c_map);
//            });
//        }
        //
        List<Map<String, String>> mapList = attrList.stream().map(baseAttrInfo -> {
            Map<String, String> c_map = new HashMap<>();
            c_map.put("attrName", baseAttrInfo.getAttrName());
            c_map.put("attrValue", baseAttrInfo.getAttrValueList().get(0).getValueName());
            return c_map;
        }).collect(Collectors.toList());
        //获取销售属性
        List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
        //获取切换功能数据
        Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
        String jsonStr = JSON.toJSONString(skuValueIdsMap);
        //存储数据
        map.put("skuInfo",skuInfo);
        map.put("categoryView",categoryView);
        map.put("price",price);
        map.put("spuPosterList",spuPosterList);
        map.put("skuAttrList",mapList);
        map.put("spuSaleAttrList",spuSaleAttrList);
        map.put("valuesSkuJson",jsonStr);

        return map;
    }
    */
}
