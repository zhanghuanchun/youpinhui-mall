package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author zhc
 * @Create 2024/8/1 18:42
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Value("${ware.url}")
    private String wareUrl;

    @Override
    public Long saveOrderInfo(OrderInfo orderInfo) {
        // total_amount
        // 单价*数量相加 订单明细集合
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        //赋值第三方交易编号
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        // 订单主体
        orderInfo.setTradeBody("电商支付");
        orderInfo.setOperateTime(new Date());
        //订单过期时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());
        //订单的进度状态
        orderInfo.setProcessStatus(OrderStatus.UNPAID.name());
        orderInfoMapper.insert(orderInfo);
        //订单明细
        List<OrderDetail> orderDetailList =
                orderInfo.getOrderDetailList();
        Long orderId = orderInfo.getId();
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setOrderId(orderId);
                orderDetailMapper.insert(orderDetail);
            }
        }
        //发送延迟消息
        rabbitService.sendDelayMsg(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,
                MqConst.ROUTING_ORDER_CANCEL,
                orderId, MqConst.DELAY_TIME);
        //返回订单id
        return orderId;
    }

    @Override
    public String getTradeNo(String userId) {
        String tradeNo = UUID.randomUUID().toString();
        String tradeNoKey = "user" + userId + ":tradNo";
        redisTemplate.opsForValue().set(tradeNoKey, tradeNo, 10, TimeUnit.MINUTES);
        return tradeNo;
    }

    @Override
    public Boolean checkTradeNo(String userId, String tradeNo) {
        String tradeNoKey = "user" + userId + ":tradNo";
        String redisTradeNo = (String) redisTemplate.opsForValue().get(tradeNoKey);
        return tradeNo.equals(redisTradeNo);
    }

    @Override
    public void delTradeNo(String userId) {
        String tradeNoKey = "user" + userId + ":tradNo";
        redisTemplate.delete(tradeNoKey);
    }

    @Override
    public Boolean checkStock(Long skuId, Integer skuNum) {
        // 远程调用http://localhost:9001/hasStock?skuId=10221&num=2
        // wareUrl = http://localhost:9001
        String result = HttpClientUtil.doGet(wareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);

        return "1".equals(result);
    }

    @Override
    public IPage<OrderInfo> getMyOrder(Page<OrderInfo> orderInfoPage, String userId, String orderStatus) {
        IPage<OrderInfo> iPage = orderInfoMapper.selectMyOrder(orderInfoPage, orderStatus, userId);
        iPage.getRecords().forEach(orderInfo -> {
            orderInfo.setOrderStatusName(OrderStatus.getStatusNameByStatus(orderInfo.getOrderStatus()));
        });

        return iPage;
    }

    /**
     * 取消订单
     *
     * @param orderId
     */
    @Override
    public void execExpiredOrder(Long orderId) {
        //取消订单本质
        // 根据订单id 修改状态！
        this.updateOrderStatus(orderId, ProcessStatus.CLOSED);
        //应该也会关闭交易记录状态 paymentInfo 使用mq 异步形式关闭交易记录
        this.rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,
                MqConst.ROUTING_PAYMENT_CLOSE,
                orderId);


    }

    /**
     * 根据订单id 修改状态！
     *
     * @param orderId
     * @param processStatus
     */
    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setProcessStatus(processStatus.name());
        orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if (orderInfo != null) {
            List<OrderDetail> orderDetails = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>().
                    eq(OrderDetail::getOrderId, orderId));
            orderInfo.setOrderDetailList(orderDetails);
        }
        return orderInfo;
    }

    @Override
    public void sendDeductStockMsg(Long orderId) {
        // 更新当前订单的状态
        this.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
        //构建发送消息的内容 json orderInfo + orderDetail
        //1.根据订单ID查询订单详情跟订单明细（商品）
        OrderInfo orderInfo = this.getOrderInfo(orderId);
        //2.将得到消息对应java对象转为Map
        Map<String, Object> wareMap = initWareOrder(orderInfo);
        //3.发送消息到RabbitMQ
        rabbitService.sendMessage(
                MqConst.EXCHANGE_DIRECT_WARE_STOCK,
                MqConst.ROUTING_WARE_STOCK,
                JSON.toJSONString(wareMap));
    }

    /**
     * 将得到订单以及明细转为Map
     *
     * @param orderInfo
     * @return
     */
    public Map<String, Object> initWareOrder(OrderInfo orderInfo) {
        Map<String, Object> mapResult = new HashMap<>();
        //从orderInfo获取封装相关订单信息
        mapResult.put("orderId", orderInfo.getId());
        mapResult.put("consignee", orderInfo.getConsignee());
        mapResult.put("consigneeTel", orderInfo.getConsigneeTel());
        mapResult.put("orderComment", orderInfo.getOrderComment());
        mapResult.put("orderBody", orderInfo.getTradeBody());
        mapResult.put("deliveryAddress", orderInfo.getDeliveryAddress());
        mapResult.put("paymentWay", "2");
        //专门给拆单使用
        mapResult.put("wareId", orderInfo.getWareId());


        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        if (!CollectionUtils.isEmpty(orderDetailList)) {
            List<Map<String, Object>> orderDetailMapList = orderDetailList.stream().map(orderDetail -> {
                Map<String, Object> detailMap = new HashMap<>();
                detailMap.put("skuId", orderDetail.getSkuId());
                detailMap.put("skuNum", orderDetail.getSkuNum());
                detailMap.put("skuName", orderDetail.getSkuName());
                return detailMap;
            }).collect(Collectors.toList());

            mapResult.put("details", orderDetailMapList);
        }
        return mapResult;
    }

    @Override
    public List<OrderInfo> split(String orderId, String wareSkuMap) {
        /**
         * 1获取原始订单
         * 2 wareSkuMap = [{"wareId":"1","skuId":["22","23"]},{}]
         * 3创建一个子订单 给子订单赋值
         * 4保存子订单
         * 5将子订单添加到集合中
         * 6修改原始订单状态
         */
        List<OrderInfo> orderInfoList = new ArrayList<>();
        OrderInfo orderInfo = getOrderInfo(Long.valueOf(orderId));
        List<Map> mapList = JSONObject.parseArray(wareSkuMap, Map.class);
        if (!CollectionUtils.isEmpty(mapList)) {
            mapList.forEach(map -> {
                //获取仓库Id
                String wareId = (String) map.get("wareId");
                List<String> skuIdList = (List<String>) map.get("skuIds");
                //创建子订单
                OrderInfo subOrderInfo = new OrderInfo();
                //属性拷贝
                BeanUtils.copyProperties(orderInfo, subOrderInfo);
                //子订单Id 设置为null 防止主键冲突
                subOrderInfo.setId(null);
                //赋值仓库id
                subOrderInfo.setWareId(wareId);
                // 指定父级订单id
                subOrderInfo.setParentOrderId(Long.valueOf(orderId));
                //计算子订单的金额    获取子订单的明细
                List<OrderDetail> orderDetailList = subOrderInfo.getOrderDetailList();
                //filter 获取子订单明细集合
                List<OrderDetail> detailList =
                        orderDetailList.stream().filter(orderDetail -> skuIdList.contains(orderDetail.getSkuId().toString())).collect(Collectors.toList());
                subOrderInfo.setOrderDetailList(detailList);
                subOrderInfo.sumTotalAmount();
                //保存子订单
                this.saveOrderInfo(subOrderInfo);
                //将子订单添加到集合
                orderInfoList.add(subOrderInfo);
            });
        }
        //修改原始订单状态
        this.updateOrderStatus(Long.valueOf(orderId), ProcessStatus.SPLIT);
        //返回子订单集合
        return orderInfoList;
    }

    @Override
    public void execExpiredOrder(Long orderId, String flag) {

        //取消订单本质
        // 根据订单id 修改状态！
        this.updateOrderStatus(orderId, ProcessStatus.CLOSED);
        if ("2".equals(flag)) {
            //应该也会关闭交易记录状态 paymentInfo 使用mq 异步形式关闭交易记录
            this.rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,
                    MqConst.ROUTING_PAYMENT_CLOSE,
                    orderId);
        }

    }
}
