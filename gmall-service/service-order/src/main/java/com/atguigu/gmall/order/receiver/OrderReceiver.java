package com.atguigu.gmall.order.receiver;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;


@Slf4j
@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentFeignClient paymentFeignClient;

    /**
     * 订单过期未支付，取消订单，修改订单信息；交易记录；AliPay交易记录。
     *
     * @param orderId 订单ID
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel) {
        try {
            if (orderId != null) {
                //根据订单id 获取订单状态
                OrderInfo orderInfo = orderService.getById(orderId);
                if (orderInfo != null && "UNPAID".equals(orderInfo.getOrderStatus())
                        && "UNPAID".equals(orderInfo.getProcessStatus())) {
                    //判断是否有交易记录
                    PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                    if (paymentInfo != null && "UNPAID".equals(paymentInfo.getPaymentStatus())) {
                        //判断是否有AliPay交易记录
                        Boolean exist = paymentFeignClient.checkPayment(orderId);
                        //判断是否存在交易记录
                        if (exist) {
                            //调用关闭交易记录方法判断
                            Boolean result = paymentFeignClient.closePay(orderId);
                            //关闭成功
                            if (result) {
                                log.info("这个订单没有支付 {}", orderInfo);
                                //说明还未支付 关闭支付宝和paymentInfo + orderInfo
                                //不存在交易记录
                                //可能需要关闭orderInfo和paymentInfo
                                orderService.execExpiredOrder(orderId, "2");
                            } else {
                                //关闭失败 代表支付成功
                                log.info("这个订单支付成功了 {}", orderInfo);
                            }
                        } else {
                            log.info("不存在支付宝交易记录，关闭 orderInfo 和 paymentInfo {}", orderInfo);
                            //不存在交易记录
                            //可能需要关闭orderInfo和paymentInfo
                            orderService.execExpiredOrder(orderId, "2");
                        }


                    } else {
                        log.info("没有本地交易记录，关闭 orderInfo {}", orderInfo);
                        //没有本地交易记录，只有orderInfo
                        //调用取消订单方法
//                        orderService.execExpiredOrder(orderId); // 更改订单状态+更改本地交易记录状态
                        orderService.execExpiredOrder(orderId, "1");
                    }


                }
            }
        } catch (Exception e) {
            log.error("取消订单异常 {} 将消息插入数据库", orderId);
            throw new RuntimeException(e);
        }
        //2.手动应答
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

    }

    /**
     * 退款成功后  关闭订单
     *
     * @param orderId
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_ORDER_CLOSED, durable = "true", autoDelete = "false")
            , exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_ORDER_CLOSED),
            key = {MqConst.ROUTING_ORDER_CLOSED}
    ))
    public void orderClosed(Long orderId, Message message, Channel channel) {
        try {
            if (orderId != null) {
                //根据订单id 获取订单状态
                OrderInfo orderInfo = orderService.getById(orderId);
                //使用业务字段保证幂等性
                if (orderInfo != null && !"CLOSED".equals(orderInfo.getOrderStatus())
                        && !"CLOSED".equals(orderInfo.getProcessStatus())) {

                    //调用关闭订单方法
                    orderService.updateOrderStatus(orderId, ProcessStatus.CLOSED);
                }
            }
        } catch (Exception e) {
            log.error("关闭订单异常 {} 将消息插入数据库" + orderId);
            throw new RuntimeException(e);
        }
        //2.手动应答
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    // 监听支付成功修改订单状态的消息

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY, durable = "true", autoDelete = "false")
            , exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void updateOrderStatus(Long orderId, Message message, Channel channel) {
        try {
            // 判断
            if (orderId != null) {
                //根据订单id 获取订单状态
                OrderInfo orderInfo = orderService.getById(orderId);
                //使用业务字段保证幂等性
                if (orderInfo != null && "UNPAID".equals(orderInfo.getOrderStatus())
                        && "UNPAID".equals(orderInfo.getProcessStatus())) {

                    //更改订单状态
                    orderService.updateOrderStatus(orderId, ProcessStatus.PAID);

                    //发送消息给库存系统
                    orderService.sendDeductStockMsg(orderId);
                }
            }
        } catch (Exception e) {
            log.error("更新订单异常 {} 将消息插入数据库" + orderId);
            throw new RuntimeException(e);
        }
        //2.手动应答
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    //监听减库存队列
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void updateOrder(String jsonStr, Message message, Channel channel) {
        try {
            //判断消息不为空
            if (!StringUtils.isEmpty(jsonStr)) {
                log.info("【订单微服务】监听到库存系统扣减库存结果：{}", jsonStr);
                //1.将扣减库存结果转为Map 得到订单ID以及扣款库存结果
                // 将 jsonStr 转换为map
                Map map = JSONObject.parseObject(jsonStr, Map.class);
                String orderId = (String) map.get("orderId");
                String status = (String) map.get("status");
                if ("DEDUCTED".equals(status)) {
                    //扣减成功
                    orderService.updateOrderStatus(Long.parseLong(orderId),
                            ProcessStatus.WAITING_DELEVER);
                } else {
                    //扣减失败-方案：提供定时任务扫描订单中处理状态为：库存异常。将这些订单作为预警订单提醒工作人员处理
                    orderService.updateOrderStatus(Long.parseLong(orderId),
                            ProcessStatus.STOCK_EXCEPTION);
                }
            }
        } catch (NumberFormatException e) {
            log.error("【订单微服务】处理扣减库存结果异常：{}", jsonStr);
            throw new RuntimeException(e);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

}
