package com.atguigu.gmall.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

/**
 * 全局过滤器 拦截所有的请求 包括静态资源 css js
 */
@Component
public class AuthFilter implements GlobalFilter {
    // 创建一个工具类对象
    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Value("${authUrls.url}")
    private String authUrls; // authUrlsUrl=trade.html,myOrder.html,list.html
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * @param exchange web请求
     * @param chain    过滤器链
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取当前用户访问的url路径
        ServerHttpRequest request = exchange.getRequest();
        // 获取到响应
        ServerHttpResponse response = exchange.getResponse();
        //真正的请求路径
        String path = request.getURI().getPath();

        if (antPathMatcher.match("/**/css/**", path) ||
                antPathMatcher.match("/**/js/**", path) ||
                antPathMatcher.match("/**/img/**", path)) {
            //放行  执行下一个过滤器
            chain.filter(exchange);
        }

        if (antPathMatcher.match("/**/inner/**", path)) {
            // 不能访问
            return out(response, ResultCodeEnum.PERMISSION);
        }
        // 获取用户ID--缓存
        String userId = getUserId(request);
        // 获取临时用户Id  --  放入请求头
        String userTempId = getUserTempId(request);

        //判断用户是否访问了带有 /auth/ 这样的路径
        if (antPathMatcher.match("/**/auth/**", path)) {
            // 同时处于为登录的情况
            if (StringUtils.isEmpty(userId)) {
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }

        //  限制用户访问哪些业务需要登录
        //  http://order.gmall.com/myOrder.html
        //  authUrlsUrl=trade.html,myOrder.html,list.html
        String[] split = authUrls.split(",");
        //  判断这个数组
        if (split.length > 0) {
            //  循环遍历
            for (String url : split) {
                //  判断是否包含
                //                if (antPathMatcher.match("/"+url,path)){
                //
                //                }
                //  没有找到返回-1 说明包含
                if (path.contains(url) && StringUtils.isEmpty(userId)) {
                    //  跳转到登录
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    //  设置跳转路径  http://passport.gmall.com/login.html?originUrl=http://www.gmall.com/
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://passport.gmall.com/login.html?originUrl=" + request.getURI());
                    //  重定向：
                    return response.setComplete();
                }
            }
        }
        if (!StringUtils.isEmpty(userId)) {
            //  请求头： HttpServletRequest request  ServerHttpRequest request
            //  将请求头中的userId ，赋值给request 对象.
            request.mutate().header("userId", userId);
        }
        if (!StringUtils.isEmpty(userTempId)) {
            //  请求头： HttpServletRequest request  ServerHttpRequest request
            //  将请求头中的userId ，赋值给request 对象.
            request.mutate().header("userTempId", userTempId);
        }

        return chain.filter(exchange);
    }

    /**
     * 获取临时用户Id
     * @param request
     * @return
     */
    private String getUserTempId(ServerHttpRequest request) {
        // 获取token
        String userTempId = "";
        HttpCookie httpCookie = request.getCookies().getFirst("userTempId");
        if (httpCookie != null) {
            userTempId = httpCookie.getValue();
        } else {
            List<String> stringList = request.getHeaders().get("userTempId");
            if (!CollectionUtils.isEmpty(stringList)) {
                userTempId = stringList.get(0);
            }
        }
        return userTempId;
    }

    /**
     * 获取用户Id
     *
     * @param request
     * @return
     */
    private String getUserId(ServerHttpRequest request) {
        // 获取token
        String token = "";
        HttpCookie httpCookie = request.getCookies().getFirst("token");
        if (httpCookie != null) {
            token = httpCookie.getValue();
        } else {
            List<String> stringList = request.getHeaders().get("token");
            if (!CollectionUtils.isEmpty(stringList)) {
                token = stringList.get(0);
            }
        }
        if (!StringUtils.isEmpty(token)) {
            String loginKey = "user:login:" + token;
            String userJson = (String) redisTemplate.opsForValue().get(loginKey);
            if (!StringUtils.isEmpty(userJson)) {
                JSONObject user = JSONObject.parseObject(userJson);
                String ip = (String) user.get("ip");
                if (ip.equals(IpUtil.getGatwayIpAddress(request))) {
                    String userId = (String) user.get("userId");
                    return userId;
                } else {
                    //非法盗用
                    return "-1";
                }
            }
        }
        return "";
    }

    /**
     * 信息提示
     *
     * @param response
     * @param permission
     * @return
     */
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum permission) {
        // 获取提示信息
        //  获取提示信息  resultCodeEnum.getMessage();
        Result<Object> result = Result.build(null, permission);
        //  result 现在是我们想要输出的内容了.
        byte[] bytes = JSONObject.toJSONString(result).getBytes();
        //  将字节数组输出出去.
        DataBuffer wrap = response.bufferFactory().wrap(bytes);
        //  指定输出的格式：中文的时候，可能会有乱码: Content-Type  java-web了解：
        response.getHeaders().add("Content-Type", "application/json;charset=utf-8");
        //  Mono.just(wrap) 返回 Mono<T>
        return response.writeWith(Mono.just(wrap));
    }
}
