package com.atguigu.gmall.product.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


/**
 * @Author zhc
 * @Create 2024/7/25 10:53
 */
@Api(tags = "后台内部数据接口")
@RestController
@RequestMapping("/api/product")
public class ProductApiController {

    @Autowired
    private ManageService manageService;

    @Autowired
    private BaseTrademarkService baseTrademarkService;
    /**
     * 根据skuId 获取到skuInfo 基本信息+图片信息
     *
     * @param skuId
     * @return
     */
    @GetMapping("/inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId) {
        // 调用服务层的方法
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        return skuInfo;
    }

    /**
     * 根据三级分类Id 查询分类数据
     *
     * @param category3Id
     * @return
     */
    @GetMapping("/inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id) {
        return manageService.getCategoryView(category3Id);
    }

    /**
     * 根据skuid 获取商品的最新价格
     *
     * @param skuId
     * @return
     */
    @GetMapping("/inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId) {
        return manageService.getSkuPrice(skuId);
    }

    /**
     * 根据spuId 获取海报信息
     *
     * @param spuId
     * @return
     */
    @GetMapping("/inner/findSpuPosterBySpuId/{spuId}")
    public List<SpuPoster> getSpuPosterBySpuId(@PathVariable Long spuId) {
        //  调用服务层方法.
        return manageService.getSpuPosterBySpuId(spuId);
    }

    /**
     * 根据SkuID查询当前商品包含平台属性以及属性值
     *
     * @param skuId
     * @return
     */
    @GetMapping("/inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable("skuId") Long skuId) {
        return manageService.getAttrList(skuId);
    }

    /**
     * 根据spuId-skuId 查询销售属性数据
     *
     * @param skuId
     * @param spuId
     * @return
     */
    @GetMapping("/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                          @PathVariable Long spuId) {
        //  调用服务层方法.
        return manageService.getSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    /**
     * 根据spuId 获取数据
     *
     * @param spuId
     * @return
     */
    @GetMapping("/inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId) {
        //  什么样的数据才能变为Json！ map--实体类
        //  调用服务层方法.
        return manageService.getSkuValueIdsMap(spuId);
    }

    /**
     * 首页
     * @return
     */
    @GetMapping("/inner/getBaseCategoryList")
    public Result getBaseCategoryList() {
        List<JSONObject> baseCategoryList = manageService.getBaseCategoryList();
        return Result.ok(baseCategoryList);
    }
    /**
     * 根据品牌ID查询品牌信息
     *
     * @param tmId 品牌ID
     * @return
     */
    @GetMapping("/inner/getTrademark/{tmId}")
    public BaseTrademark getTrademarkById(@PathVariable("tmId") Long tmId) {
        BaseTrademark trademark = baseTrademarkService.getById(tmId);
        return trademark;
    }
}
