package com.atguigu.gmall.product.service;

/**
 * @Author zhc
 * @Create 2024/7/27 0:40
 */
public interface TestService {
    void testLock();

    void testRedissonLock();

    String read();

    void write();
}
