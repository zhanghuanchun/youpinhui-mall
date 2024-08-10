package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/*
@Author zhc
@Create 2024/7/11 17:31
*/

public interface ManageService {
    /**
     * 获取所有的一级分类接口
     * @return
     */
    List<BaseCategory1> getCategory1();

    /**
     * 根据一级分类Id 查询二级分类数据
     * @param category1Id
     * @return
     */
    List<BaseCategory2> getCategory2(Long category1Id);

    /**
     * 根据二级分类Id 查询三级分类数据
     * @param category2Id
     * @return
     */
    List<BaseCategory3> getCategory3(Long category2Id);

    /**
     * 根据分类ID 查询平台属性数据
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id);

    /**
     * 保存平台属性
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 获取到平台属性值列表
     * @param attrId
     * @return
     */
    List<BaseAttrValue> getAttrValueList(Long attrId);

    /**
     * 根据平台属性id 查询平台属性对象
     * @param attrId
     * @return
     */
    BaseAttrInfo getAttrInfo(Long attrId);

    IPage<SpuInfo> getSpuList(Page<SpuInfo> spuInfoPage, Long category3Id);

    /**
     * 获取所有销售属性数据
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * 保存spuinfo 数据
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据spuId 获取spu图片列表
     * @param spuId
     * @return
     */
    List<SpuImage> getSpuImageList(Long spuId);

    /**
     * 根据spuId 查询销售属性集合
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(Long spuId);
    /**
     * 保存sku数据
     * @param skuInfo
     * @return
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 根据三级分类Id 查询skuInfo 列表
     * @param skuInfoPage
     * @param category3Id
     * @return
     */
    IPage<SkuInfo> getSkuInfoList(Page<SkuInfo> skuInfoPage, Long category3Id);
    /**
     * 根据skuId 进行上架操作
     * @return
     */
    void onSale(Long skuId);
    /**
     * 根据skuId 进行下架操作
     * @return
     */
    void cancelSale(Long skuId);
    /**
     * 根据skuId 获取到skuInfo 基本信息+图片信息
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(Long skuId);

    /**
     * 根据三级分类Id 查询数据
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryView(Long category3Id);
    /**
     * 根据skuid 获取商品的最新价格
     * @param skuId
     * @return
     */
    BigDecimal getSkuPrice(Long skuId);
    /**
     * 根据spuId 获取海报信息
     * @param spuId
     * @return
     */
    List<SpuPoster> getSpuPosterBySpuId(Long spuId);

    /**
     * 根据SkuID查询当前商品包含平台属性以及属性值
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> getAttrList(Long skuId);
    /**
     * 根据spuId-skuId 查询销售属性数据
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);
    /**
     * 根据spuId 获取数据
     * @param spuId
     * @return
     */
    Map getSkuValueIdsMap(Long spuId);

    /**
     * 查询所有的分类数据
     * @return
     */
    List<JSONObject> getBaseCategoryList();
}
