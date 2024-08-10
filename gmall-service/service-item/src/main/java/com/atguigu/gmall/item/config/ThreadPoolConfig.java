package com.atguigu.gmall.item.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author zhc
 * @Create 2024/7/28 10:51
 */
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        //需要自定义创建线程池，为什么不用工具类创建的线程池？
        //工具类创建的线程池，最大线程数或最大队列数据为 Integer.MAX_VALUE
        //会导致内存耗尽 OOM
        // 核心线程数： CPU:n+1 IO：2*n
        // n为当前服务器的核数
        return new ThreadPoolExecutor(
                5,
                50,
                3,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(50));

    }

}
