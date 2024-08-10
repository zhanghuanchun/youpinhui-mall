package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/*
@Author zhc
@Create 2024/7/22 0:45
*/
public interface BaseCategoryTradeMarkService extends IService<BaseCategoryTrademark> {
    /**
     * 根据三级分类Id 查询品牌列表
     * @param category3Id
     * @return
     */
    List<BaseTrademark> findTradeMarkList(Long category3Id);

    /**
     * 根据三级分类Id 查询可选择的品牌列表
     * @param category3Id
     * @return
     */
    List<BaseTrademark> findCurrentTrademarkList(Long category3Id);

    /**
     * 保存分类Id 与品牌对应关系
     * @param categoryTrademarkVo
     */
    void save(CategoryTrademarkVo categoryTrademarkVo);

    /**
     * 删除品牌与分类的关系
     * @param category3Id
     * @param trademarkId
     */
    void remove(Long category3Id, Long trademarkId);

}
