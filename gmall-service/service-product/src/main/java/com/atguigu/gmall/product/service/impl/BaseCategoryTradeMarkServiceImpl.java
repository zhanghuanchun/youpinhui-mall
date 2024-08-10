package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.mapper.BaseCategoryTradeMarkMapper;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseCategoryTradeMarkService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/*
@Author zhc
@Create 2024/7/22 0:46
*/
@Service
public class BaseCategoryTradeMarkServiceImpl extends ServiceImpl<BaseCategoryTradeMarkMapper,BaseCategoryTrademark> implements BaseCategoryTradeMarkService {

    @Autowired
    private BaseCategoryTradeMarkMapper baseCategoryTradeMarkMapper;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Override
    public List<BaseTrademark> findTradeMarkList(Long category3Id) {
        try {
            // 根据三级分类Id 查询品牌Id
            // select * from base_category_trademark where category3_id = 61 and is_deleted = 0;
            LambdaQueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkLambdaQueryWrapper = new LambdaQueryWrapper<>();
            baseCategoryTrademarkLambdaQueryWrapper.eq(BaseCategoryTrademark::getCategory3Id, category3Id);
            List<BaseCategoryTrademark> baseCategoryTrademarks = baseCategoryTradeMarkMapper.selectList(baseCategoryTrademarkLambdaQueryWrapper);
            //判断
            if(!CollectionUtils.isEmpty(baseCategoryTrademarks)){
    //           // 这个集合中包含tmId
    //           List<Long> tmIdList = new ArrayList<>();
    //           baseCategoryTrademarks.forEach(baseCategoryTrademark -> {
    //               tmIdList.add(baseCategoryTrademark.getTrademarkId());
    //           });
    //            //使用map集合映射
    //            List<Long> tmIdList = baseCategoryTrademarks.stream().map(baseCategoryTrademark -> {
    //                return baseCategoryTrademark.getTrademarkId();
    //            }).collect(Collectors.toList());
                List<Long> tmIdList = baseCategoryTrademarks.stream().
                        map(baseCategoryTrademark -> baseCategoryTrademark.getTrademarkId()).
                        collect(Collectors.toList());
                //查询品牌集合列表 返回数据
                // select * from base_trademark where id in(1,2,3)
                List<BaseTrademark> baseTrademarks = baseTrademarkMapper.selectBatchIds(tmIdList);
                return baseTrademarks;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //返回空
        return null;
    }

    @Override
    public List<BaseTrademark> findCurrentTrademarkList(Long category3Id) {
        // 根据三级分类Id 查询品牌Id
        LambdaQueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkLambdaQueryWrapper = new LambdaQueryWrapper<>();
        baseCategoryTrademarkLambdaQueryWrapper.eq(BaseCategoryTrademark::getCategory3Id, category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarks = baseCategoryTradeMarkMapper.selectList(baseCategoryTrademarkLambdaQueryWrapper);
        //判断
        if(!CollectionUtils.isEmpty(baseCategoryTrademarks)){
            List<Long> tmIdList = baseCategoryTrademarks.stream().
                    map(baseCategoryTrademark -> baseCategoryTrademark.getTrademarkId()).
                    collect(Collectors.toList());
            // 查询可选择的品牌集合列表
            List<BaseTrademark> baseTrademarkList =
                    baseTrademarkMapper.selectList(null).
                    stream().filter(baseTrademark -> !tmIdList.contains(baseTrademark.getId())).
                    collect(Collectors.toList());
            //返回数据
            return baseTrademarkList;

        }
        return baseTrademarkMapper.selectList(null);
    }

    @Override
    public void save(CategoryTrademarkVo categoryTrademarkVo) {

        // 先获取到品牌Id 列表；baseCategoryTrademark (61,5) (61,6)
        List<Long> trademarkIdList = categoryTrademarkVo.getTrademarkIdList();
        // 声明一个集合
//        ArrayList<BaseCategoryTrademark> baseCategoryTrademarkList = new ArrayList<>();
//        if(!CollectionUtils.isEmpty(trademarkIdList)){
//            trademarkIdList.forEach(tmId->{
//                BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
//                baseCategoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
//                baseCategoryTrademark.setTrademarkId(tmId);
//                baseCategoryTrademarkList.add(baseCategoryTrademark);
////                baseCategoryTradeMarkMapper.insert(baseCategoryTrademark);
//            });
//        }
        List<BaseCategoryTrademark> baseCategoryTrademarkList = trademarkIdList.stream().map(tmId -> {
            BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
            baseCategoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
            baseCategoryTrademark.setTrademarkId(tmId);
            return baseCategoryTrademark;
        }).collect(Collectors.toList());
        this.saveBatch(baseCategoryTrademarkList);
    }

    @Override
    public void remove(Long category3Id, Long trademarkId) {
        LambdaQueryWrapper<BaseCategoryTrademark> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseCategoryTrademark::getCategory3Id,category3Id);
        queryWrapper.eq(BaseCategoryTrademark::getTrademarkId,trademarkId);
        baseCategoryTradeMarkMapper.delete(queryWrapper);
    }
}
