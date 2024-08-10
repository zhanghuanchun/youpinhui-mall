package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
@Author zhc
@Create 2024/7/11 17:22
*/
@Api(tags = "后台管理控制器")
@RestController //@Controller(控制器) + @ResponseBody(返回数据变成json)
@RequestMapping("/admin/product")
//@CrossOrigin
public class ManageController {

    @Autowired
    private ManageService manageService;

    /**
     * 查询所有一级分类数据
     *
     * @return
     */
    @GetMapping("/getCategory1")
    public Result getCategory1() {
        // 调用service方法获取数据
        List<BaseCategory1> baseCategory1List = manageService.getCategory1();
        return Result.ok(baseCategory1List);
    }

    /**
     * /admin/product/getCategory2/{category1Id}
     * 根据一级分类Id 查询二级分类数据
     *
     * @param category1Id
     * @return
     */
    @GetMapping("/getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable Long category1Id) {
        // 调用service方法获取数据
        List<BaseCategory2> baseCategory2List = manageService.getCategory2(category1Id);

        return Result.ok(baseCategory2List);
    }

    /**
     * /admin/product/getCategory2/{category1Id}
     * 根据二级分类Id 查询三级分类数据
     *
     * @param category2Id
     * @return
     */
    @GetMapping("/getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable Long category2Id) {
        // 调用service方法获取数据
        List<BaseCategory3> baseCategory3List = manageService.getCategory3(category2Id);

        return Result.ok(baseCategory3List);
    }

    /**
     * /admin/product/attrInfoList/{category1Id}/{category2Id}/{category3Id}
     * 根据分类ID 查询平台属性数据
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    @GetMapping("/attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result getAttrInfoList(
            @PathVariable Long category1Id,
            @PathVariable Long category2Id,
            @PathVariable Long category3Id) {
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrInfoList(category1Id,category2Id,category3Id);

        return Result.ok(baseAttrInfoList);
    }

}
