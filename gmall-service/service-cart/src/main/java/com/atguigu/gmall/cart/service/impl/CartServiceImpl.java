package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.client.ProductFeignClient;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author zhc
 * @Create 2024/7/31 23:04
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Override
    public void addToCart(Long skuId, Integer skuNum, String userId) {
        //  购物车的key
        String cartKey = getCartKey(userId);
        //  hget key field;
        CartInfo cartInfoExist = (CartInfo) redisTemplate.opsForHash().get(cartKey, skuId.toString());
        //  判断
        if (cartInfoExist != null) {
            //  说明有这个商品; 每个商品最多购买200件
            Integer num = Math.min(cartInfoExist.getSkuNum() + skuNum, 200);
            cartInfoExist.setSkuNum(num);
            //  判断选中状态；如果未选中，则改为选中状态.
            if (cartInfoExist.getIsChecked() == 0) {
                cartInfoExist.setIsChecked(1);
            }
            //  修改当前的更新时间
            cartInfoExist.setUpdateTime(new Date());
            //  再赋值一下实时价格;
            cartInfoExist.setSkuPrice(productFeignClient.getSkuPrice(skuId));
            //  更新到缓存
            //  this.redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfoExist);
        } else {
            //  获取skuInfo 对象
            SkuInfo skuInfo = this.productFeignClient.getSkuInfo(skuId);
            //  获取数据：
            cartInfoExist = new CartInfo();
            cartInfoExist.setSkuId(skuId);
            cartInfoExist.setUserId(userId);
            cartInfoExist.setSkuNum(skuNum);
            //  放入购物车时价格 - 是缓存的价格
            cartInfoExist.setCartPrice(skuInfo.getPrice());
            //  实时购物车价格
            cartInfoExist.setSkuPrice(this.productFeignClient.getSkuPrice(skuId));
            cartInfoExist.setSkuName(skuInfo.getSkuName());
            cartInfoExist.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfoExist.setCreateTime(new Date());
            cartInfoExist.setUpdateTime(new Date());
            //  将数据放入缓存。
            //  this.redisTemplate.opsForHash().put(cartKey,skuId.toString(),cartInfoExist);
        }
        //  将数据放入缓存。
        this.redisTemplate.opsForHash().put(cartKey, skuId.toString(), cartInfoExist);
        //  购物车过期时间是30天不? 根据每个用户的购买力度：
    }

    private String getCartKey(String userId) {
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }

    /*
    //没有合并的版本
    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        //登录
        String cartKey = "";
        if (!StringUtils.isEmpty(userId)) {
            cartKey = this.getCartKey(userId);
        }
        if (!StringUtils.isEmpty(userTempId)) {
            cartKey = this.getCartKey(userTempId);
        }

        cartInfoList = this.redisTemplate.opsForHash().values(cartKey);
        cartInfoList.sort((cartInfo1,cartInfo2)-> DateUtil.truncatedCompareTo(cartInfo2.getUpdateTime(),cartInfo1.getUpdateTime(), Calendar.SECOND));


        return cartInfoList;
    }
     */
    // 带合并的版本
    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        List<CartInfo> cartInfoNoLoginList = new ArrayList<>();
        if (!StringUtils.isEmpty(userTempId)) {
            String cartKey = this.getCartKey(userTempId);
            cartInfoNoLoginList = this.redisTemplate.boundHashOps(cartKey).values();
        }
        if (StringUtils.isEmpty(userId)) {
            if (!CollectionUtils.isEmpty(cartInfoNoLoginList)) {
                cartInfoNoLoginList.sort((cartInfo1, cartInfo2) -> DateUtil.truncatedCompareTo(cartInfo2.getUpdateTime(), cartInfo1.getUpdateTime(), Calendar.SECOND));
            }
            return cartInfoNoLoginList;
        }
        List<CartInfo> allCartInfoList = new ArrayList<>();
        if (!StringUtils.isEmpty(userId)) {
            String cartKey = this.getCartKey(userId);
//            List<CartInfo> cartInfoLoginList = this.redisTemplate.boundHashOps(cartKey).values();
//            if (!CollectionUtils.isEmpty(cartInfoLoginList)) {
//                //双重for循环 比较
//            }
            BoundHashOperations<String, String, CartInfo> boundHashOperations = this.redisTemplate.boundHashOps(cartKey);
            if (!CollectionUtils.isEmpty(cartInfoNoLoginList)) {
                for (CartInfo cartInfoNoLogin : cartInfoNoLoginList) {
                    if (boundHashOperations.hasKey(cartInfoNoLogin.getSkuId().toString())) {
                        CartInfo cartInfoLogin = boundHashOperations.get(cartInfoNoLogin.getSkuId().toString());
                        Integer numValue = Math.min(cartInfoLogin.getSkuNum() + cartInfoNoLogin.getSkuNum(), 200);
                        cartInfoLogin.setSkuNum(numValue);
                        cartInfoLogin.setUpdateTime(new Date());
                        if (cartInfoNoLogin.getIsChecked() == 1 && cartInfoLogin.getIsChecked() == 0) {
                            cartInfoLogin.setIsChecked(1);
                        }
                        boundHashOperations.put(cartInfoLogin.getSkuId().toString(), cartInfoLogin);
                    } else {
                        cartInfoNoLogin.setUserId(userId);
                        cartInfoNoLogin.setCreateTime(new Date());
                        cartInfoNoLogin.setUpdateTime(new Date());
                        boundHashOperations.put(cartInfoNoLogin.getSkuId().toString(), cartInfoNoLogin);
                    }
                }
                this.redisTemplate.delete(this.getCartKey(userTempId));
            }
            allCartInfoList = boundHashOperations.values();
            if (CollectionUtils.isEmpty(allCartInfoList)) {
                return new ArrayList<>();
            } else {
                allCartInfoList.sort((cartInfo1, cartInfo2) -> DateUtil.truncatedCompareTo(cartInfo2.getUpdateTime(), cartInfo1.getUpdateTime(), Calendar.SECOND));
            }
        }

        return allCartInfoList;
    }

    /**
     * 修改购物车的选中状态
     *
     * @param skuId
     * @param isChecked
     * @return
     */
    @Override
    public void checkCart(Long skuId, Integer isChecked, String userId) {
        //获取购物车的key
        String cartKey = this.getCartKey(userId);
        CartInfo cartInfo = (CartInfo) this.redisTemplate.opsForHash().get(cartKey, skuId.toString());
        if (cartInfo != null) {
            cartInfo.setIsChecked(isChecked);
        }
        this.redisTemplate.opsForHash().put(cartKey, skuId.toString(), cartInfo);
    }

    @Override
    public void allCheckCart(String userId, Integer isChecked) {
        //获取购物车的key
        String cartKey = this.getCartKey(userId);
        List<CartInfo> cartInfoList = this.redisTemplate.opsForHash().values(cartKey);
        //        for (CartInfo cartInfo : cartInfoList) {
        //            cartInfo.setIsChecked(isChecked);
        //            this.redisTemplate.opsForHash().put(cartKey, cartInfo.getSkuId().toString(), cartInfo);
        //        }
        //        Map<String, Object> map = new HashMap<>();
        //        for (CartInfo cartInfo : cartInfoList) {
        //            cartInfo.setIsChecked(isChecked);
        //            map.put(cartInfo.getSkuId().toString(), cartInfo);
        //        }
        Map<String, Object> map = cartInfoList.stream().map(cartInfo -> {
            cartInfo.setIsChecked(isChecked);
            return cartInfo;
        }).collect(Collectors.toMap(cartInfo -> cartInfo.getSkuId().toString(), cartInfo -> cartInfo));

        this.redisTemplate.opsForHash().putAll(cartKey, map);
    }

    @Override
    public void deleteCart(String userId, Long skuId) {
        //获取购物车的key
        String cartKey = this.getCartKey(userId);
        this.redisTemplate.opsForHash().delete(cartKey, skuId.toString());
    }

    @Override
    public void clearCart(String userId) {
        //删除所有购物车数据
        String cartKey = this.getCartKey(userId);
        this.redisTemplate.delete(cartKey);
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        String cartKey = this.getCartKey(userId);

        List<CartInfo> cartInfoList = redisTemplate.opsForHash().values(cartKey);
        List<CartInfo> cartInfoCheckedList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(cartInfoList)) {
            cartInfoCheckedList = cartInfoList.stream().
                    filter(cartInfo -> cartInfo.getIsChecked() == 1).collect(Collectors.toList());
        }
        return cartInfoCheckedList;
    }
}
