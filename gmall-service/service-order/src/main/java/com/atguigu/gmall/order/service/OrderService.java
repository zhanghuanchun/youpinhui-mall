package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * @Author zhc
 * @Create 2024/8/1 17:22
 */
public interface OrderService extends IService<OrderInfo> {
    Long saveOrderInfo(OrderInfo orderInfo);

    /**
     * 获取流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 比较流水号
     * @param userId
     * @param tradeNo
     * @return
     */
    Boolean checkTradeNo(String userId,String tradeNo);

    /**
     * 删除流水号
     * @param userId
     */
    void delTradeNo(String userId);

    /**
     * 校验库存
     * @param skuId
     * @param skuNum
     * @return
     */
    Boolean checkStock(Long skuId, Integer skuNum);

    /**
     * 查看我的订单
     * @param orderInfoPage
     * @param userId
     * @param orderStatus
     * @return
     */
    IPage<OrderInfo> getMyOrder(Page<OrderInfo> orderInfoPage, String userId, String orderStatus);

    void execExpiredOrder(Long orderId);

    OrderInfo getOrderInfo(Long orderId);

    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    /**
     * 发送消息给库存系统
     * @param orderId
     */
    void sendDeductStockMsg(Long orderId);

    /**
     * 将orderinfo  转换为map集合
     * @param orderInfo
     * @return
     */
    Map<String, Object> initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单方法
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> split(String orderId, String wareSkuMap);

    /**
     * 根据订单Id与标识去更新数据
     * @param orderId
     * @param s
     */
    void execExpiredOrder(Long orderId, String s);
}
