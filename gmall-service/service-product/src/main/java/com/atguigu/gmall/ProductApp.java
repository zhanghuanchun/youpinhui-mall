package com.atguigu.gmall;

import com.atguigu.gmall.common.constant.RedisConst;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ProductApp implements CommandLineRunner {
    @Autowired
    private RedissonClient redissonClient;


    public static void main(String[] args) {
        SpringApplication.run(ProductApp.class, args);
    }

    /**
     * Springboot应用初始化后会执行一次该方法
     * @param args
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {
        //初始化布隆过滤器
        //设置数据规模 误判率 预计统计元素数量为 100000，期望误差率为0.01
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        bloomFilter.tryInit(100000,0.0001);
        bloomFilter.add(new Long(21));
        bloomFilter.add(new Long(22));
        bloomFilter.add(new Long(23));
        bloomFilter.add(new Long(24));
        bloomFilter.add(new Long(25));
    }
}