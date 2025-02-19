package com.atguigu.gmall.common.constant;

/**
 * Redis常量配置类
 * set name admin
 */
public class RedisConst {

    public static final String SKUKEY_PREFIX = "sku:";
    public static final String SKUKEY_SUFFIX = ":info";
    //单位：秒
    public static final long SKUKEY_TIMEOUT = 24 * 60 * 60;
    // 定义变量，记录空对象的缓存过期时间
    public static final long SKUKEY_TEMPORARY_TIMEOUT = 10 * 60;

    //单位：秒 尝试获取锁的最大等待时间
    public static final long SKULOCK_EXPIRE_PX1 = 3;
    //单位：秒 锁的持有时间
    public static final long SKULOCK_EXPIRE_PX2 = 1;
    public static final String SKULOCK_SUFFIX = ":lock";

    public static final String USER_KEY_PREFIX = "user:";
    public static final String USER_CART_KEY_SUFFIX = ":cart";
    public static final long USER_CART_EXPIRE = 60 * 60 * 24 * 30;

    //用户登录
    public static final String USER_LOGIN_KEY_PREFIX = "user:login:";
    //    public static final String userinfoKey_suffix = ":info";
    public static final int USERKEY_TIMEOUT = 60 * 60 * 24 * 7;

    //秒杀商品前缀
    public static final String SECKILL_GOODS = "seckill:goods";
    //存储用户预下单key
    public static final String SECKILL_ORDERS = "seckill:orders";
    //存储用户真正下单的key
    public static final String SECKILL_ORDERS_USERS = "seckill:orders:users";
    //存储用户剩余商品数据key的前缀
    public static final String SECKILL_STOCK_PREFIX = "seckill:stock:";
    //存储秒杀用户key的前缀
    public static final String SECKILL_USER = "seckill:user:";
    //超时时间
    //用户锁定时间 单位：秒
    public static final int SECKILL__TIMEOUT = 60 * 60 * 1;
    //  布隆过滤器使用！
    public static final String SKU_BLOOM_FILTER="sku:bloom:filter";


}
