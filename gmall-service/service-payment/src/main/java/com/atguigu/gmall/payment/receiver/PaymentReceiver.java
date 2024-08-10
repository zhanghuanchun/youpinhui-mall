package com.atguigu.gmall.payment.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.OrderFeignClient;
import com.atguigu.gmall.payment.service.PaymentService;
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

import java.nio.channels.Channels;

/**
 * @Author zhc
 * @Create 2024/8/5 21:55
 */
@Slf4j
@Component
public class PaymentReceiver {
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderFeignClient orderFeignClient;

    /**
     * 监听消息实现关闭交易记录
     *
     * @param orderId
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_CLOSE, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE),
            key = {MqConst.ROUTING_PAYMENT_CLOSE}
    ))
    public void closePaymentInfo(Long orderId, Message message, Channel channel) {
        try {
            if (orderId != null) {
                //payment_status
                //获取订单对象
                OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
                if (orderInfo != null) {
                    PaymentInfo paymentInfo = paymentService.getPaymentInfo(orderInfo.getOutTradeNo(), PaymentType.ALIPAY.name());
                    //利用业务字段实现消息的幂等性
                    if (paymentInfo != null && !"CLOSED".equals(paymentInfo.getPaymentStatus())) {
                        //关闭交易记录
                        paymentService.closePaymentInfo(paymentInfo.getId());
                    }

                }
            }
        } catch (Exception e) {
            log.error("关闭交易记录失败 {}", orderId);
            throw new RuntimeException(e);
        }
        //手动消息消费
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }


}
