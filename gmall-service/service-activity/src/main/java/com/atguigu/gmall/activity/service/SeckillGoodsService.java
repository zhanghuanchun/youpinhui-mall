package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.atguigu.gmall.model.order.OrderInfo;

import java.util.List;
import java.util.Map;

/**
 * @Author zhc
 * @Create 2024/8/7 21:49
 */
public interface SeckillGoodsService {
    List<SeckillGoods> findAll();

    SeckillGoods getSeckillGoods(Long skuId);

    /**
     * 预下单处理
     * @param skuId
     * @param skuIdStr
     * @param userId
     * @return
     */
    Result seckillOrder(Long skuId, String skuIdStr, String userId);

    void seckillUser(UserRecode userRecode);

    /**
     * 实现减库存
     * @param skuId
     */
    void seckillStock(Long skuId);

    /**
     * 检查用户秒杀商品结果
     * @param userId
     * @param skuId
     * @return
     */
    Result checkOrder(String userId, Long skuId);

    /**
     * 获取秒杀结算页数据
     * @param userId
     * @return
     */
    Map<String, Object> seckillTradeData(String userId);

}
