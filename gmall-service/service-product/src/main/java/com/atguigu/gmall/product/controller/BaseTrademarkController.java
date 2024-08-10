package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/*
@Author zhc
@Create 2024/7/21 19:53
*/
@Api(tags = "品牌管理")
@RestController
@RequestMapping("/admin/product/baseTrademark")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    /**
     * 分页查询
     * @param page 表示第几页
     * @param limit 每页显示的条数
     * @return
     */
    @GetMapping("/{page}/{limit}")
    public Result getBaseTradeMarkList(@PathVariable Long page,
                                       @PathVariable Long limit){
        // mybatis-plus 提供好了分页查询方法，返回IPage 接口：IPage 接口下实现类Page
        // select * from sku_info limit 0,3
        Page<BaseTrademark> baseTrademarks = new Page<>(page, limit);
        IPage iPage =  baseTrademarkService.getBaseTradeMarkList(baseTrademarks);
        return Result.ok(iPage);
    }

    /**
     * 删除品牌
     * @param id
     * @return
     */
    @DeleteMapping("/remove/{id}")
    public Result removeBaseTradeMarkById(@PathVariable Long id){
        //调用服务层方法
        this.baseTrademarkService.removeById(id);
        return Result.ok();
    }

    /**
     * 根据id查询品牌对象
     * @param id
     * @return
     */
    @GetMapping("/get/{id}")
    public Result getBaseTradeMarkById(@PathVariable Long id){
        //调用服务层方法
        BaseTrademark baseTrademark = this.baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }

    /**
     * 修改品牌数据
     * @param baseTrademark
     * @return
     */
    @PutMapping("/update")
    public Result updateBaseTradeMark(@RequestBody BaseTrademark baseTrademark){
        this.baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }

    /**
     * 保存品牌数据
     * @param baseTrademark
     * @return
     */
    @PostMapping("/save")
    public Result saveBaseTradeMark(@RequestBody BaseTrademark baseTrademark){
        this.baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }


}
