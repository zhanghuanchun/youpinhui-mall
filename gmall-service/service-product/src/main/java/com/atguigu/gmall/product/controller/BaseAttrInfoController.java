package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

/*
@Author zhc
@Create 2024/7/19 18:10
*/
@RestController
@RefreshScope
@RequestMapping("/admin/product")
public class BaseAttrInfoController {

    @Autowired
    private ManageService manageService;

    /**
     * 保存平台属性
     * @RequestBody ： 将前端传递的数据变成实体类
     * @param baseAttrInfo
     * @return
     */
    @PostMapping("/saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }
    /**
     * 获取到平台属性值列表
     * @param attrId
     * @return
     */
    @GetMapping("/getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable Long attrId){

        // 调用服务层方法
        BaseAttrInfo baseAttrInfo = this.manageService.getAttrInfo(attrId);
        //List<BaseAttrValue> baseAttrValues = this.manageService.getAttrValueList(attrId);
        //return Result.ok(baseAttrValues);
        //返回平台属性值集合
        return Result.ok(baseAttrInfo.getAttrValueList());
    }

}
