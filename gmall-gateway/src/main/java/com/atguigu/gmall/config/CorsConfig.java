package com.atguigu.gmall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.server.WebFilter;

/*
@Author zhc
@Create 2024/7/19 17:21
*/
@Configuration
public class CorsConfig {
    // 编写一个webFilter 将这个对象直接注入到spring容器中就行了
    @Bean
    public WebFilter webFilter(){
        // 创建corsConfiguration 对象
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*");    // 允许跨域得url
        corsConfiguration.addAllowedMethod("*");    // 允许跨域得方式
        corsConfiguration.addAllowedHeader("*");    // 允许跨域携带请求头
        corsConfiguration.setAllowCredentials(true);//允许携带 cookie 数据
        //UrlBasedCorsConfigurationSource
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource
                = new UrlBasedCorsConfigurationSource();
        //CorsConfiguration
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(urlBasedCorsConfigurationSource);
    }


}
