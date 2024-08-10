package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.CacheHelper;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.atguigu.gmall.model.order.OrderDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author zhc
 * @Create 2024/8/7 21:50
 */
@Slf4j
@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Override
    public List<SeckillGoods> findAll() {
        //查询所有数据 hvals key
        List<SeckillGoods> seckillGoodsList =
                redisTemplate.opsForHash().
                        values(RedisConst.SECKILL_GOODS);
        return seckillGoodsList;
    }

    @Override
    public SeckillGoods getSeckillGoods(Long skuId) {
        // 查询秒杀对象数据 hget key field;
        SeckillGoods seckillGoods =
                (SeckillGoods) redisTemplate.opsForHash().get(RedisConst.SECKILL_GOODS, skuId.toString());
        return seckillGoods;
    }

    @Override
    public Result seckillOrder(Long skuId, String skuIdStr, String userId) {
        //校验抢购码
        if (!skuIdStr.equals(MD5.encrypt(userId))) {
            return Result.fail().message("校验抢购码失败");
        }
        //校验状态位
        String status = (String) CacheHelper.get(skuId.toString());
        if (StringUtils.isEmpty(status) || "0".equals(status)) {
            return Result.fail().message("非法请求或商品已经售清");
        } else {
            //可以秒杀
            //创建对象存储谁购买的商品
            UserRecode userRecode = new UserRecode();
            userRecode.setSkuId(skuId);
            userRecode.setUserId(userId);
            //把这个对象放入mq队列里
            rabbitService.sendMessage(
                    MqConst.EXCHANGE_DIRECT_SECKILL_USER,
                    MqConst.ROUTING_SECKILL_USER,
                    userRecode
            );
            //初步预下单成功
            return Result.ok();
        }
    }

    /**
     * 监听预下单数据处理
     *
     * @param userRecode
     */
    @Override
    public void seckillUser(UserRecode userRecode) {
        //先判断状态位
        //校验状态位
        String status = (String) CacheHelper.get(userRecode.getSkuId().toString());
        if (StringUtils.isEmpty(status) || "0".equals(status)) {
            return;
        }
        //判断用户是否下过订单 setnx key value
        // key = seckill:user:userId =一个用户只能秒杀一件商品
        // key = seckill:user:userId:skuId =一个用户能秒杀不同的商品
        String userKey = RedisConst.SECKILL_USER + userRecode.getUserId() + userRecode.getSkuId();
        Boolean exist = redisTemplate.opsForValue().setIfAbsent(userKey, userRecode.getUserId(), RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        //表示当前用户已经购买过这个商品了 exist == true 表示插入成功 数据库里没有数据
        if (!exist) {
            return;
        }
        //redis 中是否有足够的库存！ redis-list; rpop key
        String existSkuId = (String) redisTemplate.opsForList().rightPop(RedisConst.SECKILL_STOCK_PREFIX + userRecode.getSkuId());
        if (StringUtils.isEmpty(existSkuId)) {
            //说明当前商品已经售罄
            //通知其他redis集群 该商品已经售罄
            redisTemplate.convertAndSend("seckillpush",
                    userRecode.getSkuId() + "0");
            return;
        }
        //上述校验成功，保存预下单数据
        OrderRecode orderRecode = new OrderRecode();
        orderRecode.setUserId(userRecode.getUserId());
        // 防止超卖 每次给1件
        orderRecode.setNum(1);
        orderRecode.setSeckillGoods(this.getSeckillGoods(userRecode.getSkuId()));
        // 下单码
        orderRecode.setOrderStr(MD5.encrypt(userRecode.getUserId() + userRecode.getSkuId()));
        //将上述对象存储到缓存中 hset key field value
        redisTemplate.opsForHash().put(RedisConst.SECKILL_ORDERS, userRecode.getUserId(), orderRecode);


        //修改剩余库存数量 异步： 发送消息 redis + mysql
        rabbitService.sendMessage(
                MqConst.EXCHANGE_DIRECT_SECKILL_STOCK,
                MqConst.ROUTING_SECKILL_STOCK,
                userRecode.getSkuId()
        );
    }

    /**
     * 秒杀减库存
     *
     * @param skuId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void seckillStock(Long skuId) {
        try {
            //mysql + redis
            //hget key field
            //获取当前剩余库存
            Long stockCount = redisTemplate.opsForList().size(RedisConst.SECKILL_STOCK_PREFIX + skuId);
            SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.opsForHash().get(RedisConst.SECKILL_GOODS, skuId.toString());
            //修改剩余库存
            seckillGoods.setStockCount(stockCount.intValue());
            //修改数据库
            seckillGoodsMapper.updateById(seckillGoods);
            //hset key field value;
            redisTemplate.opsForHash().put(RedisConst.SECKILL_GOODS, skuId.toString(), seckillGoods);

            log.info("修改剩余库存成功 {}" + skuId);

        } catch (Exception e) {
            log.info("修改剩余库存失败{}", skuId);
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查订单状态
     *
     * @param userId
     * @param skuId
     * @return
     */
    @Override
    public Result checkOrder(String userId, Long skuId) {
        //1.判断用户在缓存是否存在
        String userKey = RedisConst.SECKILL_USER + userId + skuId;
        Boolean exist = redisTemplate.hasKey(userKey);
        // exist = true 说明用户在缓存中存在
        if (exist) {
            //2. 判断用户是否抢单成功 --> 有预下单数据
            OrderRecode orderRecode = (OrderRecode) redisTemplate.opsForHash().get(RedisConst.SECKILL_ORDERS, userId);
            if (orderRecode != null) {
                //说明抢购成功
                Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }
        }
        // 3 判断用户是否下过订单 用户已经提交过订单 存储一个真正下单的key
        // key = seckill:orders:users field = userId value = orderId hget key field
        String orderKey = RedisConst.SECKILL_ORDERS_USERS;
        String orderIdStr = (String) redisTemplate.opsForHash().get(orderKey, userId);
        //有下过订单记录
        if (!StringUtils.isEmpty(orderIdStr)) {
            // 说明已经下过订单
            return Result.build(orderIdStr, ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }
        //4 判断状态位
        String status = (String) CacheHelper.get(skuId.toString());
        if (StringUtils.isEmpty(status) || "0".equals(status)) {
            return Result.build(orderIdStr, ResultCodeEnum.SECKILL_FAIL);
        }

        return Result.build(orderIdStr, ResultCodeEnum.SECKILL_RUN);
    }


    /**
     * 获取秒杀结算页数据
     *
     * @param userId
     * @return
     */
    @Override
    public Map<String, Object> seckillTradeData(String userId) {
        // detailArrayList :订单明细 OrderDetail totalAmount totalNum
        Map<String, Object> map = new HashMap<>();
        //通过userId 获取到预下单的数据
        OrderRecode orderRecode = (OrderRecode) redisTemplate.opsForHash().get(RedisConst.SECKILL_ORDERS, userId);

        if (orderRecode == null) {
            throw new RuntimeException("缓存中不存在预下单数据");
        }

        //获取秒杀商品对象
        SeckillGoods seckillGoods = orderRecode.getSeckillGoods();
        //需要将秒杀商品变成OrderDetail
        //2.2 构建订单明细
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillGoods.getSkuId());
        orderDetail.setSkuName(seckillGoods.getSkuName());
        orderDetail.setSkuNum(orderRecode.getNum());
        orderDetail.setOrderPrice(seckillGoods.getCostPrice());
        orderDetail.setImgUrl(seckillGoods.getSkuDefaultImg());
        //声明一个集合来存储订单明细
        List<OrderDetail> detailArrayList = new ArrayList<>();
        detailArrayList.add(orderDetail);
        //map存储数据
        map.put("detailArrayList", detailArrayList);

        map.put("totalNum", orderRecode.getNum());
        //存储总价格 每次秒杀商品只有一个 一个商品的总价
        map.put("totalAmount", seckillGoods.getCostPrice());

        return map;
    }
}
