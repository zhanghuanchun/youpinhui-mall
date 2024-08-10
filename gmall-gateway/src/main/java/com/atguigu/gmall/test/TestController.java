package com.atguigu.gmall.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author zhc
 * @Create 2024/8/4 16:51
 */
@RestController
public class TestController {
    @GetMapping("/test")
    public String test() {
        return "hello";
    }
}
