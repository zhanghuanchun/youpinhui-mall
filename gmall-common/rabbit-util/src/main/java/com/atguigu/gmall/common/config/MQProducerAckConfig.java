package com.atguigu.gmall.common.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.model.GmallCorrelationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @Description 消息发送确认-确保生产者消息不丢失
 * <p>
 * ConfirmCallback  只确认消息是否正确到达 Exchange 中
 * ReturnCallback   消息没有正确到达队列时触发回调，如果正确到达队列不执行
 * <p>
 * 1. 如果消息没有到exchange,则confirm回调,ack=false
 * 2. 如果消息到达exchange,则confirm回调,ack=true
 * 3. exchange到queue成功,则不回调return
 * 4. exchange到queue失败,则回调return
 */
@Slf4j
@Component
public class MQProducerAckConfig implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 应用启动后触发一次
     */
    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnCallback(this);
    }

    /**
     * 只确认消息是否正确到达 Exchange 中,成功与否都会回调
     *
     * @param correlationData 相关数据  非消息本身业务数据
     * @param ack             应答结果
     * @param cause           如果发送消息到交换器失败，错误原因
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            //消息到交换器成功
            log.info("消息发送到Exchange成功：{}", correlationData);
        } else {
            //消息到交换器失败
            log.error("消息发送到Exchange失败：{}", cause);
            this.retrySendMsg(correlationData);
        }
    }

    private void retrySendMsg(CorrelationData correlationData) {
        //获取重试的次数
        GmallCorrelationData gmallCorrelationData
                = (GmallCorrelationData) correlationData;
        int retryCount = gmallCorrelationData.getRetryCount();
        if (retryCount > 3) {
            log.error("消息重试次数已到 {}" + retryCount);
        } else {
            //重发次数+1
            retryCount += 1;
            gmallCorrelationData.setRetryCount(retryCount);
            redisTemplate.opsForValue().set(gmallCorrelationData.getId(), JSON.toJSONString(gmallCorrelationData), 10, TimeUnit.MINUTES);
            log.info("进行消息重发！");
            if (gmallCorrelationData.isDelay()) {
                //发延迟消息
                rabbitTemplate.convertAndSend(
                        gmallCorrelationData.getExchange(),
                        gmallCorrelationData.getRoutingKey(),
                        gmallCorrelationData.getMessage(),
                        (message -> {
                            //设置消息ttl 10s
                            message.getMessageProperties().
                                    setDelay(1000 * gmallCorrelationData.getDelayTime());
                            return message;
                        }), gmallCorrelationData);

            } else {
                //重发消息
                rabbitTemplate.convertAndSend(
                        gmallCorrelationData.getExchange(),
                        gmallCorrelationData.getRoutingKey(),
                        gmallCorrelationData.getMessage(),
                        gmallCorrelationData);

            }

        }


    }

    /**
     * 消息没有正确到达队列时触发回调，如果正确到达队列不执行
     *
     * @param message    消息对象
     * @param replyCode  应答码
     * @param replyText  应答提示信息
     * @param exchange   交换器
     * @param routingKey 路由键
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        log.error("消息路由queue失败，应答码={}，原因={}，交换机={}，路由键={}，消息={}",
                replyCode, replyText, exchange, routingKey, message.toString());
        String redisKey = message.getMessageProperties().getHeader("spring_returned_message_correlation");
        String correlationDataStr = (String) redisTemplate.opsForValue().get(redisKey);
        GmallCorrelationData gmallCorrelationData = JSON.parseObject(correlationDataStr, GmallCorrelationData.class);
        //if是延迟消息 就 return
        if (gmallCorrelationData.isDelay()) return;

        this.retrySendMsg(gmallCorrelationData);
    }
}
