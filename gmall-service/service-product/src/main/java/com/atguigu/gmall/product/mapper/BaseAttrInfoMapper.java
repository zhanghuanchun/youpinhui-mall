package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/*
@Author zhc
@Create 2024/7/12 10:34
*/
public interface BaseAttrInfoMapper extends BaseMapper<BaseAttrInfo> {
    /**
     * 根据分类ID 查询平台属性数据
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> selectAttrInfoList(@Param("category1Id") Long category1Id,
                                          @Param("category2Id") Long category2Id,
                                          @Param("category3Id") Long category3Id);

    List<BaseAttrInfo> selectAttrList(Long skuId);
}
