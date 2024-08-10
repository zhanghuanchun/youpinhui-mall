package com.atguigu.gmall.order.mapper;

import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

/**
 * @Author zhc
 * @Create 2024/8/1 19:17
 */
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
    IPage<OrderInfo> selectMyOrder(@Param("orderInfoPage") Page<OrderInfo> orderInfoPage, @Param("orderStatus") String orderStatus, @Param("userId") String userId);
}
