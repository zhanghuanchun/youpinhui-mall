package com.atguigu.gmall.client;

import com.atguigu.gmall.client.impl.ProductDegradeFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * value: 微服务的名称
 * fallback:远程调用失败的时候会 走熔断降级
 */
@FeignClient(value = "service-product", fallback = ProductDegradeFeignClient.class)
public interface ProductFeignClient {
    //调用哪个接口

    /**
     * 编写映射接口 - 控制器全路径
     *
     * @param skuId
     * @return
     */
    @GetMapping("/api/product/inner/getSkuInfo/{skuId}")
    SkuInfo getSkuInfo(@PathVariable Long skuId);

    /**
     * 根据三级分类Id 查询分类数据
     *
     * @param category3Id
     * @return
     */
    @GetMapping("/api/product/inner/getCategoryView/{category3Id}")
    BaseCategoryView getCategoryView(@PathVariable Long category3Id);

    /**
     * 根据skuid 获取商品的最新价格
     *
     * @param skuId
     * @return
     */
    @GetMapping("/api/product/inner/getSkuPrice/{skuId}")
    BigDecimal getSkuPrice(@PathVariable Long skuId);

    /**
     * 根据spuId 获取海报信息
     *
     * @param spuId
     * @return
     */
    @GetMapping("/api/product/inner/findSpuPosterBySpuId/{spuId}")
    List<SpuPoster> getSpuPosterBySpuId(@PathVariable Long spuId);

    /**
     * 根据SkuID查询当前商品包含平台属性以及属性值
     *
     * @param skuId
     * @return
     */
    @GetMapping("/api/product/inner/getAttrList/{skuId}")
    List<BaseAttrInfo> getAttrList(@PathVariable("skuId") Long skuId);

    /**
     * 根据spuId-skuId 查询销售属性数据
     * @param skuId
     * @param spuId
     * @return
     */
    @GetMapping("/api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                   @PathVariable Long spuId);

    /**
     * 根据spuId 获取数据
     * @param spuId
     * @return
     */
    @GetMapping("/api/product/inner/getSkuValueIdsMap/{spuId}")
    Map getSkuValueIdsMap(@PathVariable Long spuId);

    /**
     * 首页
     * @return
     */
    @GetMapping("/api/product/inner/getBaseCategoryList")
    Result getBaseCategoryList();

    /**
     * 根据品牌ID查询品牌信息
     *
     * @param tmId 品牌ID
     * @return
     */
    @GetMapping("/api/product/inner/getTrademark/{tmId}")
    BaseTrademark getTrademarkById(@PathVariable("tmId") Long tmId);
}
