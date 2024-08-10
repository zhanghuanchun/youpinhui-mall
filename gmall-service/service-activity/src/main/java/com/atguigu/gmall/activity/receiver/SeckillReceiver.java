package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import java.util.Date;
import java.util.List;

/**
 * @author: atguigu
 * @create: 2023-01-27 21:38
 */
@Slf4j
@Component
public class SeckillReceiver {


    @Autowired
    private SeckillGoodsService seckillGoodsService;
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;




    @Autowired
    private RedisTemplate redisTemplate;

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1,
                    durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void importSeckillGoodsToRedis(Message message, Channel channel) {
        try {
            // 扫描当天秒杀的商品导入Redis.
            // 审核状态 status = 1；商品的剩余数量：stock_count
            //  条件当天 ，剩余库存>0 , 审核状态 = 1
            LambdaQueryWrapper<SeckillGoods> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SeckillGoods::getStatus, "1").gt(SeckillGoods::getStockCount, 0);
            // select  DATE_FORMAT(start_time,'%Y-%m-%d') from seckill_goods; yyyy-mm-dd
            queryWrapper.apply("DATE_FORMAT(start_time,'%Y-%m-%d') = " + "'" + DateUtil.formatDate(new Date()) + "'");
            List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(queryWrapper);

            //将数据放入缓存
            for (SeckillGoods seckillGoods : seckillGoodsList) {
                //缓存的数据类型 hash hset key field value; hget key field
                String seckillKey = RedisConst.SECKILL_GOODS;
                // 判断缓存中是否有数据
                Boolean exist = redisTemplate.opsForHash().hasKey(seckillKey,
                        seckillGoods.getSkuId().toString());
                if (exist) {
                    // 不能覆盖
                    log.info("当前商品已经存在", seckillGoods.getSkuId());
                    continue;
                }
                redisTemplate.opsForHash().put(seckillKey,
                        seckillGoods.getSkuId().toString(), seckillGoods);
                // 存储一个商品的剩余库存数量 ：存储到redis-list 队列中
                for (Integer i = 0; i < seckillGoods.getStockCount(); i++) {
                    // lpush key value rpush key value rpop key lpop key;
                    //lpush seckill:stock:skuId skuId --> 为什么不适用 incr key
                    redisTemplate.opsForList().leftPush(
                            RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId(),
                            seckillGoods.getSkuId()
                    );
                }
                redisTemplate.convertAndSend("seckillpush", seckillGoods.getSkuId() + "1");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //签收消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    /**
     * 监听预下单队列中的信息
     *
     * @param userRecode
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void seckillUser(UserRecode userRecode, Message message, Channel channel) {
        try {
            //判断
            if (userRecode != null) {
                log.info("seckillUser 处理队列中的信息成功");
                //业务处理
                seckillGoodsService.seckillUser(userRecode);

            }
        } catch (Exception e) {
            log.error("商品预热失败 {}",e.getMessage());
            throw new RuntimeException(e);
        }
        //手动签收
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_STOCK, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_STOCK),
            key = {MqConst.ROUTING_SECKILL_STOCK}
    ))
    public void seckillStock(Long skuId, Message message, Channel channel) {
        try {
            //判断
            if (skuId != null) {
                log.info("seckillStock 监听秒杀减库存");
                //业务处理
                seckillGoodsService.seckillStock(skuId);

            }
        } catch (Exception e) {
            log.error("监听秒杀减库存 {}",e.getMessage());
            throw new RuntimeException(e);
        }
        //手动签收
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}