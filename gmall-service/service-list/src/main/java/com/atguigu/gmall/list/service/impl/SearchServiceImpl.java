package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.client.ProductFeignClient;
import com.atguigu.gmall.list.rep.GoodsRep;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SkuInfo;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @Author zhc
 * @Create 2024/7/28 22:16
 */
@Service
public class SearchServiceImpl implements SearchService {

    //  ElasticsearchRestTemplate
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    // 这个接口中有操作esCRUD方法
    @Autowired
    private GoodsRep goodsRep;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 商品上架
     *
     * @param skuId
     * @return
     */
    @Override
    public void upperGoods(Long skuId) {
        // 创建goods对象
        Goods goods = new Goods();
        //多线程查询商品数据进行上架操作！
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            // 查询skuInfo
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            goods.setId(skuId);
            //商品价格 skuInfo.getPrice() --> 可能来自于缓存，可能出现在某一瞬间数据不一致
            //直接查询数据库
            goods.setPrice(productFeignClient.getSkuPrice(skuId).doubleValue());
            goods.setTitle(skuInfo.getSkuName());
            goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            goods.setCreateTime(new Date());
            return skuInfo;
        });
        //获取品牌数据
        CompletableFuture<Void> trademarkByIdCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //查询品牌数据
            BaseTrademark trademark = productFeignClient.getTrademarkById(skuInfo.getTmId());
            goods.setTmId(trademark.getId());
            goods.setTmName(trademark.getTmName());
            goods.setTmLogoUrl(trademark.getLogoUrl());
        });
        //获取分类数据
        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            goods.setCategory1Id(categoryView.getCategory1Id());
            goods.setCategory2Id(categoryView.getCategory2Id());
            goods.setCategory3Id(categoryView.getCategory3Id());
            goods.setCategory1Name(categoryView.getCategory1Name());
            goods.setCategory2Name(categoryView.getCategory2Name());
            goods.setCategory3Name(categoryView.getCategory3Name());
        });
        CompletableFuture<Void> attrListCompletableFuture = CompletableFuture.runAsync(() -> {
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
            List<SearchAttr> searchAttrList = attrList.stream().map(baseAttrInfo -> {
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(baseAttrInfo.getId());
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                searchAttr.setAttrValue(baseAttrInfo.getAttrValueList().get(0).getValueName());
                return searchAttr;
            }).collect(Collectors.toList());
            goods.setAttrs(searchAttrList);
        });
        CompletableFuture.allOf(
                skuInfoCompletableFuture,
                trademarkByIdCompletableFuture,
                categoryViewCompletableFuture,
                attrListCompletableFuture
        ).join();
        goodsRep.save(goods);

    }

    /**
     * 商品下架
     *
     * @param skuId
     * @return
     */
    @Override
    public void lowerGoods(Long skuId) {
        goodsRep.deleteById(skuId);
    }

    @Override
    public void incrHotScore(Long skuId) {
        //借助redis zset-排序 zincrby key 1 skuId
        String key = "hotScore";
        Double score = this.redisTemplate.opsForZSet().incrementScore(key, "skuId:" + skuId, 1);
        if (score % 10 == 0) {
            Optional<Goods> optional = goodsRep.findById(skuId);
            Goods goods = optional.get();
            goods.setHotScore(score.longValue());
            this.goodsRep.save(goods);
        }
    }

    /**
     * 商品检索
     *
     * @param searchParam
     * @return
     */
    @Override
    public SearchResponseVo search(SearchParam searchParam) {
        // 1 生成DSL语句
        SearchRequest searchRequest = this.buildDSL(searchParam);

        // 2 执行DSL语句
        SearchResponse searchResponse = null;
        try {
            searchResponse = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 3 获取执行结果转换为SearchResponseVo实体
        SearchResponseVo searchResponseVo = this.parseResult(searchResponse);
        /*
        private Integer pageSize;//每页显示的内容
        private Integer pageNo;//当前页面
        private Long totalPages;//总页数
         */
        //每页显示的条数
        searchResponseVo.setPageSize(searchParam.getPageSize());
        //当前页面
        searchResponseVo.setPageNo(searchParam.getPageNo());
        //总页数
        Long totalPages = (searchResponseVo.getTotal() + searchParam.getPageSize() - 1) / searchParam.getPageSize();
        searchResponseVo.setTotalPages(totalPages);
        //返回总页数
        return searchResponseVo;
    }

    private SearchRequest buildDSL(SearchParam searchParam) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 判断用户是否根据分类Id 查询 分类ID查询入口
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            boolQueryBuilder.filter(QueryBuilders.
                    termQuery("category1Id", searchParam.getCategory1Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            boolQueryBuilder.filter(QueryBuilders.
                    termQuery("category2Id", searchParam.getCategory2Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            boolQueryBuilder.filter(QueryBuilders.
                    termQuery("category3Id", searchParam.getCategory3Id()));
        }

        // 根据关键词查询
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            boolQueryBuilder.must(QueryBuilders.
                    matchQuery("title", searchParam.getKeyword()).
                    operator(Operator.AND));
            //无检索不高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("title");
            highlightBuilder.preTags("<span style=color:red>");
            highlightBuilder.postTags("</span>");
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        // 用户可以通过品牌进行过滤
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)) {
            // 1:小米
            String[] trademarkValues = trademark.split(":");
            if (trademarkValues != null && trademarkValues.length == 2) {
                boolQueryBuilder.filter(QueryBuilders.
                        termQuery("tmId", trademarkValues[0]));
            }
        }
        // 用户根据平台属性过滤 nested;
        // props = 24:128G:机身内存&props=23:4G:运行内存
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                String[] split = prop.split(":");
                if (split != null && split.length == 3) {
                    // 创建内部的bool
                    BoolQueryBuilder innerBoolQueryBuilder = QueryBuilders.boolQuery();
                    //获取平台属性Id
                    innerBoolQueryBuilder.filter(QueryBuilders.
                            termQuery("attrs.attrId", split[0]));
                    //获取平台属性值
                    innerBoolQueryBuilder.filter(QueryBuilders.
                            termQuery("attrs.attrValue", split[1]));
                    boolQueryBuilder.filter(QueryBuilders.
                            nestedQuery("attrs",
                                    innerBoolQueryBuilder,
                                    ScoreMode.None));
                }
            }
        }
        // 设置分页
        int from = (searchParam.getPageNo() - 1) * searchParam.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(searchParam.getPageSize());
        //设置排序
        //获取用户的排序规则
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)) {
            String[] split = order.split(":");
            if (split != null && split.length == 2) {
                String orderField = "";
                switch (split[0]) {
                    case "1":
                        orderField = "hotScore";
                        break;
                    case "2":
                        orderField = "price";
                        break;
                }
                searchSourceBuilder.sort(orderField,
                        "asc".equals(split[1]) ? SortOrder.ASC : SortOrder.DESC);
            }
        } else {
            searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        }
        searchSourceBuilder.query(boolQueryBuilder);

        // 聚合
        searchSourceBuilder.aggregation(
                AggregationBuilders.terms("tmIdAgg").field("tmId").
                        subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName")).
                        subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl")));
        // 平台属性聚合
        searchSourceBuilder.aggregation(
                AggregationBuilders.nested("attrsAgg", "attrs")
                        .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                                .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                                .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))));
        // 查询：GET /goods/_search
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.fetchSource(new String[]{"id", "defaultImg", "title", "price"}, null);
        //打印DSL语句
        System.out.println("DSL:\t" + searchSourceBuilder.toString());

        //返回对象
        return searchRequest;
    }

    private SearchResponseVo parseResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        /*
        private List<SearchResponseTmVo> trademarkList;
        private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
        private List<Goods> goodsList = new ArrayList<>();
        private Long total;//总记录数
         */
        //赋值总记录数
        SearchHits hits = searchResponse.getHits();
        searchResponseVo.setTotal(hits.getTotalHits().value);

        // 获取商品列表
        List<Goods> goodsList = new ArrayList<>();
        // 获取商品集合数据 添加到goodsList
        SearchHit[] subHits = hits.getHits();
        //循环遍历
        for (SearchHit subHit : subHits) {
            String sourceAsString = subHit.getSourceAsString();
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            // 如果时分词查询，则商品名称必须要高亮；不能获取_source 下的title
            // 判断是否根据关键词查询
            if (subHit.getHighlightFields().get("title") != null) {
                String title = subHit.getHighlightFields().get("title").getFragments()[0].toString();
                // 将高亮的商品名称赋值给title
                goods.setTitle(title);
            }
            // 将goods添加到集合当中
            goodsList.add(goods);
        }
        // 赋值商品列表
        searchResponseVo.setGoodsList(goodsList);


        // 获取品牌数据
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        // tmIdAgg 看做一个key获取数据
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        //获取聚合中的map
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map(bucket -> {
            // 声明一个品牌对象
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            //获取品牌Id
            String tmId = bucket.getKeyAsString();
            searchResponseTmVo.setTmId(Long.parseLong(tmId));
            //获取品牌名称
            ParsedStringTerms tmNameAgg = bucket.getAggregations().get("tmNameAgg");
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);
            //获取品牌LOGO
            ParsedStringTerms tmLogoUrlAgg = bucket.getAggregations().get("tmLogoUrlAgg");
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);
            return searchResponseTmVo;
        }).collect(Collectors.toList());
        // 赋值品牌集合对象
        searchResponseVo.setTrademarkList(trademarkList);


        // 获取平台属性数据
        // attrs 数据类型是nested
        ParsedNested attrsAgg = (ParsedNested) aggregationMap.get("attrsAgg");
        // 根据key 来获取平台数据集合
        ParsedLongTerms attrIdAgg = attrsAgg.getAggregations().get("attrIdAgg");
        List<SearchResponseAttrVo> searchResponseAttrVoList = attrIdAgg.getBuckets().stream().map(bucket -> {
            //获取平台属性Id
            String attrId = bucket.getKeyAsString();
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
            searchResponseAttrVo.setAttrId(Long.parseLong(attrId));
            //获取平台属性名
            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseAttrVo.setAttrName(attrName);
            //获取平台属性值
            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attrValueAgg");
            List<String> attrValueList = attrValueAgg.getBuckets().stream().map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList());
            searchResponseAttrVo.setAttrValueList(attrValueList);

            return searchResponseAttrVo;
        }).collect(Collectors.toList());
        //赋值平台属性数据
        searchResponseVo.setAttrsList(searchResponseAttrVoList);

        return searchResponseVo;
    }


}
