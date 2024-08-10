package com.atguigu.gmall.common.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.model.GmallCorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author zhc
 * @Create 2024/8/2 23:45
 */
@Service
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 发送消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息
     */
    public boolean sendMessage(String exchange, String routingKey, Object message) {
        //创建 一个对象
        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();
        String uuid = "mq:" + UUID.randomUUID().toString().replaceAll("-", "");
        //消息的唯一标识
        gmallCorrelationData.setId(uuid);
        gmallCorrelationData.setMessage(message);
        gmallCorrelationData.setExchange(exchange);
        gmallCorrelationData.setRoutingKey(routingKey);
        //记录到redis里面
        redisTemplate.opsForValue().set(uuid, JSON.toJSONString(gmallCorrelationData), 10, TimeUnit.MINUTES);
        //调用工具类发送消息
        rabbitTemplate.convertAndSend(exchange, routingKey, message, gmallCorrelationData);
        // 默认返回true
        return true;
    }

    public Boolean sendDelayMsg(String exchange, String routingKey, Object msg, int delayTime) {

        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();

        String uuid = "mq:" + UUID.randomUUID().toString().replaceAll("-", "");
        //消息的唯一标识
        gmallCorrelationData.setId(uuid);
        gmallCorrelationData.setMessage(msg);
        gmallCorrelationData.setExchange(exchange);
        gmallCorrelationData.setRoutingKey(routingKey);
        //设置该消息为延迟消息
        gmallCorrelationData.setDelay(true);
        //延迟时间
        gmallCorrelationData.setDelayTime(delayTime);

        //记录到redis里面
        redisTemplate.opsForValue().set(uuid, JSON.toJSONString(gmallCorrelationData), 10, TimeUnit.MINUTES);
        //调用工具类发送消息
        rabbitTemplate.convertAndSend(exchange, routingKey, msg, (message -> {
            //设置消息ttl 10s
            message.getMessageProperties().setDelay(1000 * delayTime);
            return message;
        }), gmallCorrelationData);
        // 默认返回true
        return true;
    }
}
