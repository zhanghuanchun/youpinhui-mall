package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author zhc
 * @Create 2024/7/23 22:01
 */
@Api(tags = "后台商品管理控制器")
@RestController
@RequestMapping("/admin/product")
public class SkuManageController {

    @Autowired
    private ManageService manageService;

    /**
     * 根据spuId 获取spu图片列表
     *
     * @param spuId
     * @return
     */
    @GetMapping("/spuImageList/{spuId}")
    public Result getSpuImageList(@PathVariable Long spuId) {
        //调用服务层方法
        List<SpuImage> spuImageList = manageService.getSpuImageList(spuId);
        //返回数据
        return Result.ok(spuImageList);
    }

    @GetMapping("/spuSaleAttrList/{spuId}")
    public Result spuSaleAttrList(@PathVariable Long spuId) {
        //调用服务层方法
        List<SpuSaleAttr> spuSaleAttrList = this.manageService.getSpuSaleAttrList(spuId);
        //返回数据
        return Result.ok(spuSaleAttrList);
    }

    /**
     * 保存sku数据
     * @param skuInfo
     * @return
     */
    @PostMapping("/saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    /**
     * 根据三级分类Id 查询skuInfo 列表
     * @param page
     * @param limit
     * @param category3Id
     * @return
     */
    @GetMapping("/list/{page}/{limit}")
    public Result getSkuInfoList(@PathVariable Long page,
                                 @PathVariable Long limit,
                                 @RequestParam Long category3Id){
        Page<SkuInfo> skuInfoPage = new Page<>(page,limit);
        IPage<SkuInfo> iPage = this.manageService.getSkuInfoList(skuInfoPage,category3Id);
        return Result.ok(iPage);
    }

    /**
     * 根据skuId 进行上架操作
     * @return
     */
    @GetMapping("/onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        this.manageService.onSale(skuId);
        return Result.ok();
    }

    /**
     * 根据skuId 进行下架操作
     * @return
     */
    @GetMapping("/cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        this.manageService.cancelSale(skuId);
        return Result.ok();
    }

}
