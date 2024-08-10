package com.atguigu.gmall.mq.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/mq")
public class MqController {
    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 消息发送
     */
    //http://localhost:8282/api/mq/sendConfirm
    @GetMapping("sendConfirm")
    public Result sendConfirm() {
        rabbitService.sendMessage("exchange.confirm4444", "routing.confirm", "来人了，开始接客吧！");
        return Result.ok();
    }

    /**
     * 消息发送延迟消息：基于死信实现
     */
    //http://localhost:8282/api/mq/sendDeadLetterMsg
    @GetMapping("/sendDeadLetterMsg")
    public Result sendDeadLetterMsg() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("发送时间" + simpleDateFormat.format(new Date()));
        rabbitService.sendMessage(DeadLetterMqConfig.exchange_dead, DeadLetterMqConfig.routing_dead_1, "我是延迟消息");
        log.info("基于死信发送延迟消息成功");
        return Result.ok();
    }

    /**
     * 消息发送延迟消息：基于延迟插件使用
     */
    //http://localhost:8282/api/mq/sendDelayMsg
    @GetMapping("/sendDelayMsg")
    public Result sendDelayMsg() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("发送时间" + simpleDateFormat.format(new Date()));

//        rabbitTemplate.convertAndSend(DelayedMqConfig.exchange_delay,
//                DelayedMqConfig.routing_delay,
//                "基于延迟插件-我是延迟消息",
//                (message -> {
//                    //设置消息ttl 10s
//                    message.getMessageProperties().setDelay(10000);
//                    return message;
//                })
//        );
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("content","基于延迟插件-我是延迟消息");
//        jsonObject.put("uuid", UUID.randomUUID().toString());

        rabbitService.sendDelayMsg(
                DelayedMqConfig.exchange_delay,
                DelayedMqConfig.routing_delay,
                "atguigu",
                3
        );

        log.info("基于延迟插件-发送延迟消息成功");
        return Result.ok();
    }

}
