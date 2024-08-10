package com.atguigu.gmall.list.rep;

import com.atguigu.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @Author zhc
 * @Create 2024/7/28 22:22
 */
public interface GoodsRep extends ElasticsearchRepository<Goods,Long> {
}
