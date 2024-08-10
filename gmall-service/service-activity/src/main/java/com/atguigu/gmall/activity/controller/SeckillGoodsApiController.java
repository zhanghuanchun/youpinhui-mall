package com.atguigu.gmall.activity.controller;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsApiController {

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 返回全部列表
     *
     * @return
     */
    @GetMapping("/findAll")
    public Result findAll() {
        return Result.ok(seckillGoodsService.findAll());
    }

    /**
     * 获取实体
     *
     * @param skuId
     * @return
     */
    @GetMapping("/getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable("skuId") Long skuId) {
        return Result.ok(seckillGoodsService.getSeckillGoods(skuId));
    }

    /**
     * 为当前用户购买意向生成抢购码
     *
     * @param skuId 商品ID
     * @return
     */
    @GetMapping("/auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuStr(HttpServletRequest request,
                                   @PathVariable("skuId") Long skuId) {
        //生成抢购码！时间：必须时秒杀开始之后，结束之前
        //利用userId 对 userid 进行md5加密
        //获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //获取秒杀对象
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoods(skuId);
        //判断
        if (seckillGoods != null) {
            //获取当前时间
            Date currentTime = new Date();
            //判断时机
            if (DateUtil.dateCompare(seckillGoods.getStartTime(), currentTime) &&
                    DateUtil.dateCompare(currentTime, seckillGoods.getEndTime())) {
                //生成抢购码
                String skuIdStr = MD5.encrypt(userId);
                return Result.ok(skuIdStr);
            }
        }
        return Result.fail().message("生成抢购码失败");
    }

    /**
     * 秒杀请求入队
     *
     * @param request
     * @param skuId   商品ID
     * @return
     */
    @PostMapping("/auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable Long skuId, HttpServletRequest request) {

        //获取到抢购码
        String skuIdStr = request.getParameter("skuIdStr");
        //获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //调用服务层
        return seckillGoodsService.seckillOrder(skuId, skuIdStr, userId);
    }

    /**
     * 检查用户秒杀商品结果
     *
     * @param skuId
     * @param request
     * @return
     */
    @GetMapping(value = "auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable("skuId") Long skuId, HttpServletRequest request) {
        //当前登录用户
        String userId = AuthContextHolder.getUserId(request);
        return seckillGoodsService.checkOrder(userId, skuId);
    }

    /**
     * 获取秒杀结算页数据
     *
     * @return
     */
    @GetMapping("/auth/trade")
    Result<Map<String, Object>> seckillTradeData(HttpServletRequest request) {
        // userAddressList  异步获取的 不用管
        // detailArrayList totalAmount totalNum
        String userId = AuthContextHolder.getUserId(request);
        //调用服务层方法
        Map<String, Object> map = seckillGoodsService.seckillTradeData(userId);
        //返回数据
        return Result.ok(map);
    }

    /**
     * 保存秒杀订单
     *
     * @param orderInfo
     * @return
     */
    @PostMapping("/auth/submitOrder")
    public Result submitSeckillOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        //获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //给orderInfo  对象赋值用户ID
        orderInfo.setUserId(Long.valueOf(userId));
        //对于秒杀订单来讲 不需要验证流水号 价格 库存
        Long orderId = orderFeignClient.submitSeckillOrder(orderInfo);
        if (orderId!=null) {
            return Result.fail().message("提交订单失败.");
        }
        //删除预下单数据：hdel key field
        redisTemplate.opsForHash().delete(RedisConst.SECKILL_ORDERS,userId);
        //重新保存真正的订单数据到缓存
        // key = seckill:orders:users field = userId value = orderId hget key field
        redisTemplate.opsForHash().put(RedisConst.SECKILL_ORDERS_USERS,userId,orderId.toString());
        //返回订单Id
        return Result.ok(orderId);
    }

}

