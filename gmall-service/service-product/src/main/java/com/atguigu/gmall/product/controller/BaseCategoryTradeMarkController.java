package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.service.BaseCategoryTradeMarkService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
@Author zhc
@Create 2024/7/22 0:39
*/
@Api(tags = "分类与品牌管理")
@RestController
@RequestMapping("/admin/product/baseCategoryTrademark")
public class BaseCategoryTradeMarkController {
    @Autowired
    private BaseCategoryTradeMarkService baseCategoryTradeMarkService;

    /**
     * 根据三级分类Id 查询品牌列表
     * @param category3Id
     * @return
     */
    @GetMapping("/findTrademarkList/{category3Id}")
    public Result findTradeMarkList(@PathVariable Long category3Id){

        List<BaseTrademark> baseTrademarks = baseCategoryTradeMarkService.findTradeMarkList(category3Id);
        return Result.ok(baseTrademarks);
    }

    /**
     * 根据三级分类Id 查询可选择的品牌列表
     * @param category3Id
     * @return
     */
    @GetMapping("/findCurrentTrademarkList/{category3Id}")
    public Result findCurrentTrademarkList(@PathVariable Long category3Id){

        List<BaseTrademark> baseTrademarks = baseCategoryTradeMarkService.findCurrentTrademarkList(category3Id);
        return Result.ok(baseTrademarks);
    }

    /**
     * 保存分类Id 与品牌关系
     * @param categoryTrademarkVo
     * @return
     */
    @PostMapping("/save")
    public Result save(@RequestBody CategoryTrademarkVo categoryTrademarkVo){
        this.baseCategoryTradeMarkService.save(categoryTrademarkVo);
        return Result.ok();
    }

    @DeleteMapping("/remove/{category3Id}/{trademarkId}")
    public Result remove(@PathVariable Long category3Id,
                         @PathVariable Long trademarkId){
        this.baseCategoryTradeMarkService.remove(category3Id,trademarkId);
        return Result.ok();
    }


}
