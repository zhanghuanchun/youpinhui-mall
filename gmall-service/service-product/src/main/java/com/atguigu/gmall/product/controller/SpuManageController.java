package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
@Author zhc
@Create 2024/7/23 10:40
*/
@Api(tags = "后台商品管理控制器")
@RestController //@Controller(控制器) + @ResponseBody(返回数据变成json)
@RequestMapping("/admin/product")
public class SpuManageController {

    @Autowired
    private ManageService manageService;

    /**
     * 根据三级分类Id 查询spu列表
     * @param page
     * @param limit
     * @return
     */
    @GetMapping("/{page}/{limit}")
    public Result getSpuList(@PathVariable Long page,
                             @PathVariable Long limit,
                             @RequestParam Long category3Id){
        //构建Page 对象，传递page,limit
        Page<SpuInfo> spuInfoPage = new Page<>(page,limit);
        //调用服务层方法
        IPage<SpuInfo> iPage = manageService.getSpuList(spuInfoPage,category3Id);
        // 返回分页数据
        return Result.ok(iPage);
    }

    /**
     * 获取所有销售属性数据
     * @return
     */
    @GetMapping("/baseSaleAttrList")
    public Result getBaseSaleAttrList(){
        List<BaseSaleAttr> baseSaleAttrList = this.manageService.getBaseSaleAttrList();
        return Result.ok(baseSaleAttrList);
    }

    /**
     * 保存spuinfo数据
     * @param spuInfo
     * @return
     */
    @PostMapping("/saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){

        this.manageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }

}
