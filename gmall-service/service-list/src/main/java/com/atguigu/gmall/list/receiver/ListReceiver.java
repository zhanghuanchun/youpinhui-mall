package com.atguigu.gmall.list.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.list.service.SearchService;
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

/**
 * @author: atguigu
 * @create: 2023-01-13 14:19
 */
@Slf4j
@Component
public class ListReceiver {

    @Autowired
    private SearchService searchService;

    /**
     * 商品上架监听器
     *
     * @param skuId
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    public void upperGoods(Long skuId, Message message, Channel channel) {
        try {
            //1.判断商品ID是否有值
            if (skuId != null) {
                log.info("【检索微服务】监听到商品上架消息：{}", skuId);
                //2.调用业务逻辑完成商品上架
                searchService.upperGoods(skuId);

            }
        } catch (Exception e) {
            // nack() 借助redis 限制重回次数！
            // 消息没有被正确处理：1 消息标识 2 是否批量签收 3 是否重回队列
            channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
            e.printStackTrace();
            // insert into value (id ...); 消息记录表 --->人工处理
            log.error("【检索微服务】，商品上架业务异常：{}", e);
        }
        //3.手动应答rabbitmq
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    /**
     * 商品下架监听器
     *
     * @param skuId
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(
            bindings = @QueueBinding(
                    exchange = @Exchange(MqConst.EXCHANGE_DIRECT_GOODS),
                    value = @Queue(value = MqConst.QUEUE_GOODS_LOWER, durable = "true"),
                    key = MqConst.ROUTING_GOODS_LOWER
            )
    )
    public void lowerGoods(Long skuId, Message message, Channel channel) {
        try {
            //1.判断商品ID是否有值
            if (skuId != null) {
                log.info("【检索微服务】监听到商品下架消息：{}", skuId);
                //2.调用业务逻辑完成商品上架
                searchService.lowerGoods(skuId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // insert into value (id ...); 消息记录表 --->人工处理
            log.error("【检索微服务】，商品下架业务异常：{}", e);
        }
        //3.手动应答rabbitmq
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
