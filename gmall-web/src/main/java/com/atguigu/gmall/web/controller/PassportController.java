package com.atguigu.gmall.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author zhc
 * @Create 2024/7/31 20:05
 */
@Controller
public class PassportController {

    @GetMapping("login.html")
    public String login(HttpServletRequest request){
        request.setAttribute("originUrl",request.getParameter("originUrl"));
        return "login";
    }
}
