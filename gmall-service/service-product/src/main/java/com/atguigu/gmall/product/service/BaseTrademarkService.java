package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/*
@Author zhc
@Create 2024/7/21 19:57
*/
public interface BaseTrademarkService extends IService<BaseTrademark> {
    /**
     * 分页查询
     * @param baseTrademarks
     * @return
     */
    IPage getBaseTradeMarkList(Page<BaseTrademark> baseTrademarks);
}
