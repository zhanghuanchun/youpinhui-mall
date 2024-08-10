package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.cart.CartFeignClient;
import com.atguigu.gmall.client.ProductFeignClient;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @Author zhc
 * @Create 2024/8/1 17:17
 */
@RestController
@RequestMapping("/api/order")
public class OrderApiController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 订单确认页面数据模型汇总
     *
     * @param request
     * @return
     */
    @GetMapping("/auth/trade")
    public Result<Map<String, Object>> trade(HttpServletRequest request) {
        //获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //创建MAP集合
        Map<String, Object> map = new HashMap<>();
        //获取订单明细 -- 获取选中购物车列表
        List<CartInfo> cartCheckedList =
                cartFeignClient.getCartCheckedList(userId);
        List<OrderDetail> detailArrayList = new ArrayList<>();
        AtomicInteger totalNum = new AtomicInteger();
        if (!CollectionUtils.isEmpty(cartCheckedList)) {
            detailArrayList = cartCheckedList.stream().map(cartInfo -> {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setOrderPrice(cartInfo.getSkuPrice());
                // += 总件数
                totalNum.addAndGet(cartInfo.getSkuNum());

                return orderDetail;
            }).collect(Collectors.toList());
            map.put("detailArrayList", detailArrayList);
        }
        // 存储总金额 totalAmount
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(detailArrayList);
        orderInfo.sumTotalAmount();
        map.put("totalAmount", orderInfo.getTotalAmount());
        //存储总件数 totalNum
        map.put("totalNum", totalNum);
        //存储流水号
        String tradeNo = orderService.getTradeNo(userId);
        map.put("tradeNo", tradeNo);
        return Result.ok(map);
    }

    @PostMapping("/auth/submitOrder")
    public Result saveOrderInfo(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        // 获取用户ID
        String userId = AuthContextHolder.getUserId(request);
        //赋值用户id
        orderInfo.setUserId(Long.valueOf(userId));
        // 先获取到页面传递的流水号
        String tradeNo = request.getParameter("tradeNo");

        //单线程操作 多线程就可能会出现误删锁的问题
//        //调用比较的方法
//        Boolean result = orderInfoService.checkTradeNo(userId, tradeNo);
//        if (!result) {
//            //给提示信息
//            return Result.fail().message("不能重复提交");
//        }
//        //删除
//        orderInfoService.delTradeNo(userId);
        //声明一个lua脚本
        String scriptText = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                "then\n" +
                "    return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                "    return 0\n" +
                "end";
        DefaultRedisScript redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(scriptText);
        redisScript.setResultType(Long.class);
        String tradeNoKey = "user" + userId + ":tradNo";
        //执行lua脚本 键与口令串一致的时候就删除 否则就不删除
        Long result = (Long) redisTemplate.execute(redisScript, Arrays.asList(tradeNoKey), tradeNo);
        if (result == 0) {
            // 删除失败 给提示信息
            return Result.fail().message("不能重复提交");
        }
        //创建一个多线程集合对象
        List<CompletableFuture> completableFutureList = new ArrayList<>();
        //存储错误信息
        List<String> errorList = new ArrayList<>();

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        //遍历整个订单集合
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            for (OrderDetail orderDetail : orderDetailList) {

                //校验库存
                CompletableFuture<Void> stockCompletableFuture = CompletableFuture.runAsync(() -> {
                    //调用服务处方法
                    Boolean exist = this.orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                    if (!exist) {
//                        return Result.fail().message(orderDetail.getSkuId() + "库存不足");
                        errorList.add(orderDetail.getSkuId() + "库存不足");
                    }
                }, threadPoolExecutor);
                //添加到集合中
                completableFutureList.add(stockCompletableFuture);

                //校验价格
                CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
                    //获取当前订单价格
                    BigDecimal orderPrice = orderDetail.getOrderPrice();
                    //获取当前商品的最新价格
                    BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                    if (orderPrice.compareTo(skuPrice) != 0) {
                        //知道谁变动了
                        Long skuId = orderDetail.getSkuId();
                        String cartKey = RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
                        //获取当前这个购物项 hget key field
                        CartInfo cartInfo = (CartInfo) redisTemplate.opsForHash().get(cartKey, skuId.toString());
                        cartInfo.setSkuPrice(skuPrice);
                        //将最新的数据写回购物车 hset key field value
                        redisTemplate.opsForHash().put(cartKey, skuId.toString(), cartInfo);
                        //价格有变动
                        String msg = orderPrice.compareTo(skuPrice) == 1 ? ":降价了:" : ":涨价了";
                        BigDecimal price = orderPrice.subtract(skuPrice).abs();
                        errorList.add(skuId + "价格有变动" + msg + price + "元");
//                        return Result.fail().message(skuId + "价格有变动" + msg + price + "元");

                    }
                }, threadPoolExecutor);
                //添加到集合中
                completableFutureList.add(priceCompletableFuture);
            }
        }
        CompletableFuture.allOf(
                completableFutureList.toArray(new CompletableFuture[completableFutureList.size()])
        ).join();
        if (errorList.size() > 0) {
            return Result.fail().message(StringUtils.join(errorList, ","));
        }

        Long orderId = this.orderService.saveOrderInfo(orderInfo);
        return Result.ok(orderId);
    }

    /**
     * 查看我的订单
     *
     * @param page
     * @param limit
     * @param request
     * @return
     */
    @GetMapping("/auth/{page}/{limit}")
    public Result getOrder(@PathVariable Long page,
                           @PathVariable Long limit,
                           HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        //获取订单状态
        String orderStatus = request.getParameter("orderStatus");
        //这是分页查询
        Page<OrderInfo> orderInfoPage = new Page<>(page, limit);
        IPage<OrderInfo> iPage = this.orderService.getMyOrder(orderInfoPage, userId, orderStatus);
        return Result.ok(iPage);
    }

    /**
     * 内部调用获取订单
     *
     * @param orderId
     * @return
     */
    @GetMapping("/inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable(value = "orderId") Long orderId) {
        //获取orderInfo + orderDetail
        return orderService.getOrderInfo(orderId);
    }

    @PostMapping("/orderSplit")
    public List<Map<String, Object>> orderSplit(HttpServletRequest request) {
        //获取参数
        String orderId = request.getParameter("orderId");
        // [{"wareId":"1","skuId":["22","23"]},{}]
        String wareSkuMap = request.getParameter("wareSkuMap");

        // 调用服务层方法 获取orderInfo 构成的子订单
        List<OrderInfo> orderInfoList = this.orderService.split(orderId, wareSkuMap);
        //改造我们需要的数据 获取map构成的子订单
        List<Map<String, Object>> mapList =
                orderInfoList.stream().map(orderInfo -> orderService.initWareOrder(orderInfo)).collect(Collectors.toList());

        return mapList;
    }

    /**
     * 秒杀提交订单，秒杀订单不需要做前置判断，直接下单
     *
     * @param orderInfo
     * @return
     */
    @PostMapping("/inner/seckill/submitOrder")
    Long submitSeckillOrder(@RequestBody OrderInfo orderInfo){
        //调用服务层的方法
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return orderId;
    }
}
