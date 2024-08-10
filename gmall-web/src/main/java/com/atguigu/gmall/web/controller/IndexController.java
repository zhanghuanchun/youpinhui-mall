package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.client.ProductFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.FileWriter;
import java.io.IOException;

/**
 * @Author zhc
 * @Create 2024/7/28 16:01
 */
@Controller
public class IndexController {
    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private TemplateEngine templateEngine;

    //首页的控制器 \ 或 index.html
    @GetMapping(value = {"/", "index.html" })
    public String index(Model model) {
        Result result = productFeignClient.getBaseCategoryList();
        model.addAttribute("list", result.getData());
        return "index/index";
    }

    @GetMapping("createIndex")
    @ResponseBody
    public Result createIndex(){
        //静态化数据是从远程调用来的
        Result result = productFeignClient.getBaseCategoryList();

        //创建一个对象context 页面要显示的内容
        Context context = new Context();
        context.setVariable("list",result.getData());
        // 创建一个输出流
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("D:\\index.html");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        templateEngine.process("index/index.html",context,fileWriter);

        return Result.ok();
    }
}
