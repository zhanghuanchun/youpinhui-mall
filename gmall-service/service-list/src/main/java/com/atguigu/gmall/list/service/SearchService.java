package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

/**
 * @Author zhc
 * @Create 2024/7/28 22:14
 */
public interface SearchService {
    /**
     * 测商品上架
     * @param skuId
     * @return
     */
    void upperGoods(Long skuId);
    /**
     * 商品下架
     * @param skuId
     * @return
     */
    void lowerGoods(Long skuId);

    /**
     * 热度排名
     * @param skuId
     * @return
     */
    void incrHotScore(Long skuId);
    /**
     * 商品检索
     * @param searchParam
     * @return
     */
    SearchResponseVo search(SearchParam searchParam);
}
