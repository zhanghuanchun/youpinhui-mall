package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/*
@Author zhc
@Create 2024/7/21 20:08
*/
@Service
public class BaseTrademarkServiceImpl extends ServiceImpl<BaseTrademarkMapper,BaseTrademark> implements BaseTrademarkService {

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;


    @Override
    public IPage getBaseTradeMarkList(Page<BaseTrademark> baseTrademarks) {
        // select * from base_trademark order by id desc limit 0,3;
        // 构建排序条件
        LambdaQueryWrapper<BaseTrademark> baseTrademarkLambdaQueryWrapper = new LambdaQueryWrapper<>();
        baseTrademarkLambdaQueryWrapper.orderByDesc(BaseTrademark::getId);
        return baseTrademarkMapper.selectPage(baseTrademarks, baseTrademarkLambdaQueryWrapper);
    }
}
