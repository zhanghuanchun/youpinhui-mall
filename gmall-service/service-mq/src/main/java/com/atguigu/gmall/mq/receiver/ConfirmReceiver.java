package com.atguigu.gmall.mq.receiver;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ConfirmReceiver {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 监听消息
     * 1. 初始化绑定关系
     * 2. 监听消息
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm", durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "exchange.confirm"),
            key = {"routing.confirm"}
    ))
    public void getMsg(String msg, Message message, Channel channel) {
        System.out.println("接受的消息" + msg);
//        System.out.println("接受的消息" + new String(message.getBody()));
        //手动确认
        //第一个参数 消息标识
        //第二哥参数 是否批量签收
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    /**
     * 监听延迟消息
     *
     * @param msg
     * @param message
     * @param channel
     */
    @RabbitListener(queues = {DeadLetterMqConfig.queue_dead_2})
    public void getDeadLetterMsg(String msg, Message message, Channel channel) {
        try {
            if (StringUtils.isNotBlank(msg)) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                System.out.println("接收时间" + simpleDateFormat.format(new Date()));
                log.info("死信消费者：{}", msg);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("[xx服务]监听xxx业务异常：{}", e);
        }
    }

    /**
     * 监听插件延迟消息
     *
     * @param msg
     * @param message
     * @param channel
     */
    @RabbitListener(queues = {DelayedMqConfig.queue_delay_1})
    public void getDelayMsg(String msg, Message message, Channel channel) {
        try {
            if (StringUtils.isNotBlank(msg)) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Boolean result = redisTemplate.opsForValue().setIfAbsent("uuid", "atguigu", 3, TimeUnit.MINUTES);
                if (!result) {
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    return;
                }

                System.out.println("接收时间" + simpleDateFormat.format(new Date()));
                log.info("插件延迟消息：{}", msg);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            redisTemplate.delete("uuid");
            e.printStackTrace();
            log.error("[xx服务]监听xxx业务异常：{}", e);
        }
    }


}
