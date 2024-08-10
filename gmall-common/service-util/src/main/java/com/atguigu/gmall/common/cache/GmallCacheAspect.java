package com.atguigu.gmall.common.cache;

import com.atguigu.gmall.common.constant.RedisConst;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @Author zhc
 * @Create 2024/7/27 19:33
 */
@Component
@Aspect
public class GmallCacheAspect {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     *
     * @param joinPoint 获取请求之前的参数 请求的方法题 返回值
     * @return
     */
    @SneakyThrows
    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object cacheAspect(ProceedingJoinPoint joinPoint){
        //声明一个对象
        Object obj = new Object();
        //实现分布式锁业务逻辑
        //获取缓存的Key:注解前缀 + 参数 + 注解后最
        MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
        GmallCache gmallCache = methodSignature.getMethod().getAnnotation(GmallCache.class);
        //前缀
        String prefix = gmallCache.prefix();
        //后缀
        String suffix = gmallCache.suffix();
        //获取参数
        Object[] args = joinPoint.getArgs();
        // 组成缓存key
        String skuKey = prefix + Arrays.asList(args)+ suffix;

        try {
            // 从缓存中获取数据
            obj = this.redisTemplate.opsForValue().get(skuKey);
            //
            if(obj == null){
                // 查询数据库：前面加一把锁! 前缀不能重复
                String locKey = prefix + ":lock";
                RLock lock = this.redissonClient.getLock(locKey);
                //上锁
                lock.lock();
                try {
                    // 业务逻辑
                    // 缓存中没有数据
                    obj = joinPoint.proceed(args);
                    if(obj == null){
                        String strEmpty = new String();
                        this.redisTemplate.opsForValue().
                                set(skuKey,strEmpty,RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);

                        return strEmpty;
                    }else {
                        this.redisTemplate.opsForValue().
                                set(skuKey,obj,RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        return obj;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    //释放锁资源
                    lock.unlock();
                }

            }else {
                return obj;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return joinPoint.proceed(args);
    }


}
