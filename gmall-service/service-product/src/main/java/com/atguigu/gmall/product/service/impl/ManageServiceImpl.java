package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/*
@Author zhc
@Create 2024/7/11 17:33
*/
@Service
public class ManageServiceImpl implements ManageService {

    //查询所有一级分类数据 base_category1
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private BaseAttrValueService baseAttrValueService;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SpuPosterMapper spuPosterMapper;

    @Autowired
    private SpuImageService spuImageService;

    @Autowired
    private SpuPosterService spuPosterService;

    @Autowired
    private SpuSaleAttrService spuSaleAttrService;

    @Autowired
    private SpuSaleAttrValueService spuSaleAttrValueService;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private SkuInfoService skuInfoService;

    @Autowired
    private SkuImageService skuImageService;

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        // select * from base_category2 where category1_id = ?;
        // QueryWrapper<BaseCategory2> baseCategory2QueryWrapper = new QueryWrapper<>();
        // baseCategory2QueryWrapper.eq("category1_id",category1Id);
        LambdaQueryWrapper<BaseCategory2> baseCategory2LambdaQueryWrapper = new LambdaQueryWrapper<>();
        baseCategory2LambdaQueryWrapper.eq(BaseCategory2::getCategory1Id, category1Id);

        return baseCategory2Mapper.selectList(baseCategory2LambdaQueryWrapper);
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {

        LambdaQueryWrapper<BaseCategory3> baseCategory3LambdaQueryWrapper = new LambdaQueryWrapper<>();
        baseCategory3LambdaQueryWrapper.eq(BaseCategory3::getCategory2Id, category2Id);
        return baseCategory3Mapper.selectList(baseCategory3LambdaQueryWrapper);
    }

    /**
     * 根据分类ID 查询平台属性数据
     *
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        /*
        select
            bai.id,bai.attr_name,bai.category_id,bai.category_level,
            bav.id,bav.value_name
        from base_attr_info bai
                          inner join base_attr_value bav
                                     on bai.id = bav.attr_id
        where ((category_id = 2 and category_level = 1)
            or (category_id = 13 and category_level = 2)
            or (category_id = 61 and category_level = 3));
         */
        return baseAttrInfoMapper.selectAttrInfoList(category1Id, category2Id, category3Id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //base_attr_info 插入完成之后，这个表的Id就有了
        //@TabledId(type = IdType.AUTO) 获取主键自增
        //判断是新增还是更新
        if (baseAttrInfo.getId() != null) {
            //修改base_attr_info
            this.baseAttrInfoMapper.updateById(baseAttrInfo);
            //删除：base_attr_info 数据
            LambdaQueryWrapper<BaseAttrValue> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(BaseAttrValue::getAttrId, baseAttrInfo.getId());
            this.baseAttrValueMapper.delete(queryWrapper);

        } else {
            //新增
            baseAttrInfoMapper.insert(baseAttrInfo);
        }
        //新增：base_attr_value
        //获取到所有的平台属性值集合
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        //判断
        if (!CollectionUtils.isEmpty(attrValueList)) {
            attrValueList.forEach(baseAttrValue -> {
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                //baseAttrValueMapper.insert(baseAttrValue);
            });
            // insert into values (?,?),(?,?),(?,?)
            //批量插入，借助IService ServiceImpl
            this.baseAttrValueService.saveBatch(attrValueList);
        }

    }

    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {
        // select * from base_attr_value where attr_id = ?
        LambdaQueryWrapper<BaseAttrValue> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseAttrValue::getAttrId, attrId);
        return baseAttrValueMapper.selectList(queryWrapper);
    }

    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        BaseAttrInfo baseAttrInfo = this.baseAttrInfoMapper.selectById(attrId);
        if (baseAttrInfo != null) {
            baseAttrInfo.setAttrValueList(this.getAttrValueList(attrId));
        }
        return baseAttrInfo;
    }

    @Override
    public IPage<SpuInfo> getSpuList(Page<SpuInfo> spuInfoPage, Long category3Id) {
        // 构建查询语句，select * from spu_info where category3_Id = 61 order by id desc limit 1
        LambdaQueryWrapper<SpuInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SpuInfo::getCategory3Id, category3Id).
                orderByDesc(SpuInfo::getId);
        return this.spuInfoMapper.selectPage(spuInfoPage, queryWrapper);
    }

    /**
     * 获取所有销售属性数据
     *
     * @return
     */
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return this.baseSaleAttrMapper.selectList(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSpuInfo(SpuInfo spuInfo) {
        /*
            1.考虑将数据保存到哪些表中
             spu_info
             spu_image
             spu_poster
             spu_sale_attr
             spu_sale_attr_value
            2.创建对应的mapper

            3.利用mapper将数据存储的表中

            4.事务
         */
        //spu_info
        spuInfoMapper.insert(spuInfo);
        //spu_image
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (!CollectionUtils.isEmpty(spuImageList)) {
            spuImageList.forEach(spuImage -> {
                spuImage.setSpuId(spuInfo.getId());
//                spuImageMapper.insert(spuImage);
            });
            this.spuImageService.saveBatch(spuImageList);
        }
        //spu_poster
        List<SpuPoster> spuPosterList = spuInfo.getSpuPosterList();
        if (!CollectionUtils.isEmpty(spuPosterList)) {
            spuPosterList.forEach(spuPoster -> {
                spuPoster.setSpuId(spuInfo.getId());
//                spuPosterMapper.insert(spuPoster);
            });
            this.spuPosterService.saveBatch(spuPosterList);
        }
        //spu_sale_attr
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (!CollectionUtils.isEmpty(spuSaleAttrList)) {
            spuSaleAttrList.forEach(spuSaleAttr -> {
                spuSaleAttr.setSpuId(spuInfo.getId());
//                spuSaleAttrMapper.insert(spuSaleAttr);
                //spu_sale_attr_value
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)) {
                    spuSaleAttrValueList.forEach(spuSaleAttrValue -> {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
//                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    });
                    this.spuSaleAttrValueService.saveBatch(spuSaleAttrValueList);
                }
            });
            this.spuSaleAttrService.saveBatch(spuSaleAttrList);
        }


    }

    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        LambdaQueryWrapper<SpuImage> spuImageLambdaQueryWrapper = new LambdaQueryWrapper<>();
        spuImageLambdaQueryWrapper.eq(SpuImage::getSpuId, spuId);
        spuImageLambdaQueryWrapper.orderByDesc(SpuImage::getSpuId);
        return spuImageMapper.selectList(spuImageLambdaQueryWrapper);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSkuInfo(SkuInfo skuInfo) {
        skuInfoMapper.insert(skuInfo);
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)) {
            skuImageList.forEach(skuImage -> {
                skuImage.setSkuId(skuInfo.getId());
            });
            //批量保存
            this.skuImageService.saveBatch(skuImageList);
        }
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)) {
            skuAttrValueList.forEach(skuAttrValue -> {
                skuAttrValue.setSkuId(skuInfo.getId());
            });
            //批量保存
            this.skuAttrValueService.saveBatch(skuAttrValueList);
        }

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {
            skuSaleAttrValueList.forEach(skuSaleAttrValue -> {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
            });
            //批量保存
            this.skuSaleAttrValueService.saveBatch(skuSaleAttrValueList);
        }
        // 添加商品的时候，将这个skuId  添加到布隆过滤器中
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        bloomFilter.add(skuInfo.getId());
    }

    @Override
    public IPage<SkuInfo> getSkuInfoList(Page<SkuInfo> skuInfoPage, Long category3Id) {
        LambdaQueryWrapper<SkuInfo> skuInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        skuInfoLambdaQueryWrapper.
                eq(SkuInfo::getCategory3Id, category3Id).
                orderByDesc(SkuInfo::getId);
        return skuInfoMapper.selectPage(skuInfoPage, skuInfoLambdaQueryWrapper);
    }

    @Override
    public void onSale(Long skuId) {
        // 为了 保证mysql与redis 数据一致性 采用延迟双删 sku:[23]:info - 缓存的key
        String key = "sku:[" + skuId + "]:info";
        //删除缓存数据
        this.redisTemplate.delete(key);
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        this.skuInfoMapper.updateById(skuInfo);
        // 睡眠：防止其他线程将旧数据放入缓存中
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        this.redisTemplate.delete(key);
        //发送消息 内容根据消费者定
        this.rabbitService.sendMessage(
                MqConst.EXCHANGE_DIRECT_GOODS,
                MqConst.ROUTING_GOODS_UPPER,
                skuId);
    }

    @Override
    public void cancelSale(Long skuId) {
        // 为了 保证mysql与redis 数据一致性 采用延迟双删 sku:[23]:info - 缓存的key
        String key = "sku:[" + skuId + "]:info";
        //删除缓存数据
        this.redisTemplate.delete(key);
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        this.skuInfoMapper.updateById(skuInfo);
        // 睡眠：防止其他线程将旧数据放入缓存中
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        this.redisTemplate.delete(key);
        //发送消息 内容根据消费者定
        this.rabbitService.sendMessage(
                MqConst.EXCHANGE_DIRECT_GOODS,
                MqConst.ROUTING_GOODS_LOWER,
                skuId);
    }

    @Override
    @GmallCache(prefix = "sku:")
    public SkuInfo getSkuInfo(Long skuId) {
        return getSkuInfoDB(skuId);
//        return getSkuInfoByRedis(skuId);
//        return getSkuInfoByRedisson(skuId);
    }


    private SkuInfo getSkuInfoByRedisson(Long skuId) {
        //  声明一个对象
        SkuInfo skuInfo = null;
        try {
            // 缓存要存储数据 --- 1. 数据类型 string  hash set list zset 数据类型使用场景
            // skuInfo 数据使用 Hash! --> 不太合适，修改比较少 可以使用string
            // 先判断缓存中是否有数据！组成缓存的Key! key名不能重复！
            // sku:skuId:info
            String skuKey =
                    RedisConst.SKUKEY_PREFIX +
                            skuId + RedisConst.SKUKEY_SUFFIX;
            // 从缓存中获取数据
            skuInfo = (SkuInfo) this.redisTemplate.opsForValue().get(skuKey);

            if (skuInfo == null) {
                // redisson --- key
                // 考虑高并发，缓存击穿：分布式锁！ sku:skuId:lock
                String skuLocKey =
                        RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                RLock lock = redissonClient.getLock(skuLocKey);
                //上锁
                lock.lock();
                try {
                    // 业务逻辑
                    // 缓存中没有数据
                    skuInfo = getSkuInfoDB(skuId);
                    if (skuInfo == null) {
                        //  设置一个空对象
                        SkuInfo skuInfoEmpty = new SkuInfo();
                        //  放入缓存
                        this.redisTemplate.opsForValue().
                                set(skuKey, skuInfoEmpty, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);

                        return skuInfoEmpty;
                    }
                    //  放入缓存
                    this.redisTemplate.opsForValue().
                            set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    return skuInfo;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    //释放锁资源
                    lock.unlock();
                }
            } else {
                // 返回缓存数据
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getSkuInfoDB(skuId);
    }

    /**
     * 根据skuId 查询skuInfo 数据 -- redis
     *
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoByRedis(Long skuId) {
        //  声明一个对象
        SkuInfo skuInfo = null;
        try {
            // 缓存要存储数据 --- 1. 数据类型 string  hash set list zset 数据类型使用场景
            // skuInfo 数据使用 Hash! --> 不太合适，修改比较少 可以使用string
            // 先判断缓存中是否有数据！组成缓存的Key! key名不能重复！
            // sku:skuId:info
            String skuKey =
                    RedisConst.SKUKEY_PREFIX +
                            skuId + RedisConst.SKUKEY_SUFFIX;
            // 从缓存中获取数据
            skuInfo = (SkuInfo) this.redisTemplate.opsForValue().get(skuKey);
            if (skuInfo == null) {
                // 考虑高并发，缓存击穿：分布式锁！ sku:skuId:lock
                String skuLocKey =
                        RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                // 生成一个随机的uuid
                String uuid = UUID.randomUUID().toString();
                // set key value ex timeout nx;
                Boolean result =
                        redisTemplate.opsForValue().
                                setIfAbsent(skuLocKey, uuid, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                //  判断获取锁结果：
                if (result) {
                    //  获取成功！
                    //  缓存中没有数据。
                    skuInfo = getSkuInfoDB(skuId);
                    if (skuInfo == null) {
                        //  设置一个空对象
                        SkuInfo skuInfoEmpty = new SkuInfo();
                        //  放入缓存
                        this.redisTemplate.opsForValue().
                                set(skuKey, skuInfoEmpty, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                        // 调用释放资源
                        this.expireKey(skuLocKey, uuid);
                        return skuInfoEmpty;
                    }
                    //  放入缓存
                    this.redisTemplate.opsForValue().
                            set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    // 调用释放资源
                    this.expireKey(skuLocKey, uuid);
                    return skuInfo;
                } else {
                    // 获取失败
                    Thread.sleep(300);
                    return getSkuInfo(skuId);
                }
            } else {
                //  返回缓存中的数据
                return skuInfo;
            }
        } catch (InterruptedException e) {
            //  日志输出.
            e.printStackTrace();
        }
        // 如果有异常直接访问数据库！
        return getSkuInfoDB(skuId);
    }

    /**
     * 根据skuId 查询skuInfo 数据 -- mysql
     *
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoDB(Long skuId) {
        //根据主键查询数据
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        LambdaQueryWrapper<SkuImage> skuImageLambdaQueryWrapper = new LambdaQueryWrapper<>();
        skuImageLambdaQueryWrapper.eq(SkuImage::getSkuId, skuId);
        List<SkuImage> skuImageList = skuImageMapper.selectList(skuImageLambdaQueryWrapper);
        if (skuInfo != null) {
            skuInfo.setSkuImageList(skuImageList);
        }
        return skuInfo;
    }

    private void expireKey(String skuLocKey, String uuid) {
        String scriptText = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                "then\n" +
                "    return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                "    return 0\n" +
                "end";
        //设置Lua脚本
        DefaultRedisScript redisScript = new DefaultRedisScript();
        redisScript.setScriptText(scriptText);
        redisScript.setResultType(Long.class);
        // lua脚本,key,value
        this.redisTemplate.
                execute(redisScript, Arrays.asList(skuLocKey), uuid);
    }

    @Override
    @GmallCache(prefix = "categoryView:")
    public BaseCategoryView getCategoryView(Long category3Id) {
        return baseCategoryViewMapper.selectById(category3Id);
    }

    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        //声明一个分布式key
        String locKey = skuId + ":lock";
        RLock lock = redissonClient.getLock(locKey);
        //上锁
        lock.lock();
        try {
            //SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
            LambdaQueryWrapper<SkuInfo> skuInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
            skuInfoLambdaQueryWrapper.eq(SkuInfo::getId, skuId).select(SkuInfo::getPrice);
            SkuInfo skuInfo = skuInfoMapper.selectOne(skuInfoLambdaQueryWrapper);
            if (skuInfo != null) {
                return skuInfo.getPrice();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return new BigDecimal("0");
    }

    @Override
    @GmallCache(prefix = "spuPoster")
    public List<SpuPoster> getSpuPosterBySpuId(Long spuId) {
        LambdaQueryWrapper<SpuPoster> spuPosterLambdaQueryWrapper = new LambdaQueryWrapper<>();
        spuPosterLambdaQueryWrapper.eq(SpuPoster::getSpuId, spuId);
        return spuPosterMapper.selectList(spuPosterLambdaQueryWrapper);
    }

    @Override
    @GmallCache(prefix = "attrList")
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return baseAttrInfoMapper.selectAttrList(skuId);
    }

    @Override
    @GmallCache(prefix = "spuSaleAttrList")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    @Override
    @GmallCache(prefix = "skuValueIdsMap")
    public Map getSkuValueIdsMap(Long spuId) {
        HashMap<Object, Object> map = new HashMap<>();
        List<Map> mapList = skuSaleAttrValueMapper.selectSkuValueIdsMap(spuId);
        mapList.forEach(map1 -> {
            map.put(map1.get("values_ids"), map1.get("sku_id"));
        });
        return map;
    }

    @Override
    @GmallCache(prefix = "index:")
    public List<JSONObject> getBaseCategoryList() {
        // 创建一个Json集合对象
        List<JSONObject> list = new ArrayList<>();
        // 封装分类数据 数据来源---三个分类表视图
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        // 按照一级分类Id 进行分组
        // key = category1Id value = List<BaseCategoryView>
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //声明一个index变量
        int index = 1;
        Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator = category1Map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, List<BaseCategoryView>> entry = iterator.next();
            Long category1Id = entry.getKey();
            List<BaseCategoryView> category1ViewList = entry.getValue();
            // 声明一个一级分类数据对象
            JSONObject category1 = new JSONObject();
            category1.put("index", index);
            category1.put("categoryId", category1Id);
            category1.put("categoryName", category1ViewList.get(0).getCategory1Name());
            // 根据二级分类Id  进行分组
            // key = category2Id value = List<BaseCategoryView>
            Map<Long, List<BaseCategoryView>> category2Map = category1ViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator1 = category2Map.entrySet().iterator();
            //声明一个二级分类数据集合
            ArrayList<JSONObject> category2ChildList = new ArrayList<>();
            while (iterator1.hasNext()) {
                Map.Entry<Long, List<BaseCategoryView>> entry1 = iterator1.next();
                Long category2Id = entry1.getKey();
                List<BaseCategoryView> category2ViewList = entry1.getValue();
                // 声明一个二级分类数据对象
                JSONObject category2 = new JSONObject();
                category2.put("categoryId", category2Id);
                category2.put("categoryName", category2ViewList.get(0).getCategory2Name());
                //声明一个三级分类数据集合
                ArrayList<JSONObject> category3ChildList = new ArrayList<>();
                // 获取三级分类数据
                category2ViewList.stream().forEach(baseCategoryView -> {
                    // 声明一个三级分类数据对象
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId", baseCategoryView.getCategory3Id());
                    category3.put("categoryName", baseCategoryView.getCategory3Name());
                    category3ChildList.add(category3);
                });
                //将三级分类数据集合放入二级分类数据中
                category2.put("categoryChild", category3ChildList);
                category2ChildList.add(category2);
            }
            //将二级分类数据集合放入一级分类数据中
            category1.put("categoryChild", category2ChildList);
            list.add(category1);
            //变量迭代
            index++;
        }
        return list;
    }
}
