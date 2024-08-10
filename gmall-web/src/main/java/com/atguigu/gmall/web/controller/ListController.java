package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchAttr;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author zhc
 * @Create 2024/7/30 9:55
 */
@Controller
public class ListController {

    @Autowired
    private ListFeignClient listFeignClient;

    /**
     * 商品检索控制器
     *
     * @param searchParam
     * @return
     */
    @GetMapping("list.html")
    public String list(SearchParam searchParam, Model model) {
        //  远程调用
        Result<Map> result = listFeignClient.list(searchParam);
        //  页面渲染需要存储key ${searchParam} ${urlParam} ${trademarkParam} ${propsParamList} ${orderMap} - 排序
        //  ${trademarkList} ${attrsList} ${goodsList} ${pageNo} ${totalPages } 它都是属于searchResponseVo的属性！
        //  获取品牌面包屑： 品牌：品牌名称
        String trademarkParam = this.makeTradeMarkParam(searchParam.getTrademark());
        //  获取平台属性面包屑 平台属性名:平台属性值  存储一个集合
        List<SearchAttr> propsParamList = this.makePropsList(searchParam.getProps());
        //  存储排序规则： orderMap.type orderMap.sort  class {private String type ; private String sort; }  定义一个map
        //  order=2:desc
        Map orderMap = this.makeOrderMap(searchParam.getOrder());
        //  获取用户检索条件
        String urlParam = this.makeUrlParma(searchParam);
        //  存储用户根据哪些条件进行检索！
        model.addAttribute("urlParam", urlParam);
        //  存储品牌面包屑
        model.addAttribute("trademarkParam", trademarkParam);
        //  存储平台属性面包屑
        model.addAttribute("propsParamList", propsParamList);
        //存储排序规则Map orderMap = this.makeOrderMap(searchParam.getOrder());
        model.addAttribute("orderMap", orderMap);
        //  存储用户检索的条件
        model.addAttribute("searchParam", searchParam);
        //   ${trademarkList} ${attrsList} ${goodsList} ${pageNo} ${totalPages } 它都是属于searchResponseVo的属性！
        model.addAllAttributes(result.getData());
        //  返回检索列表页面
        return "list/index";
    }

    /**
     * 存储排序规则
     *
     * @param order order=2:asc
     * @return
     */
    private Map makeOrderMap(String order) {
        //  创建一个map 集合
        Map map = new HashMap();
        //  判断 order=2:desc
        if (!StringUtils.isEmpty(order)) {
            //  分割数据
            String[] split = order.split(":");
            if (split != null && split.length == 2) {
                //   orderMap.type orderMap.sort  type = 1 综合 type = 2 价格 sort
                map.put("type", split[0]);
                map.put("sort", split[1]);
            }
        } else {
            //  默认排序规则
            map.put("type", "1");
            map.put("sort", "desc");
        }
        //  返回map即可
        return map;
    }

    /**
     * 制作平台属性面包屑
     *
     * @param props
     * @return
     */
    private List<SearchAttr> makePropsList(String[] props) {
        //  创建集合对象
        List<SearchAttr> searchAttrList = new ArrayList<>();
        //  判断
        if (props != null && props.length > 0) {
            //  循环遍历整个数组
            for (String prop : props) {
                //  1次循环 23:8G:运行内存  2 次循环 24:128G:机身内存
                //  将prop 进行分割
                String[] split = prop.split(":");
                if (split != null && split.length == 3) {
                    //  创建对象
                    SearchAttr searchAttr = new SearchAttr();
                    //  赋值平台属性Id
                    searchAttr.setAttrId(Long.parseLong(split[0]));
                    //  赋值平台属性值名
                    searchAttr.setAttrValue(split[1]);
                    //  赋值平台属性名
                    searchAttr.setAttrName(split[2]);
                    //  将对象添加到集合中
                    searchAttrList.add(searchAttr);
                }
            }
        }
        //  返回面包屑集合
        return searchAttrList;
    }

    /**
     * 获取品牌面包屑
     *
     * @param trademark = 1:小米
     * @return
     */
    private String makeTradeMarkParam(String trademark) {
//  判断
        if (!StringUtils.isEmpty(trademark)) {
            //  字符串分割
            String[] split = trademark.split(":");
            if (split != null && split.length == 2) {
                //  返回面包屑
                return "品牌：" + split[1];
            }
        }
        return "";
    }

    /**
     * 记录用户的检索条件
     *
     * @param searchParam
     * @return
     */
    private String makeUrlParma(SearchParam searchParam) {
        //  list.html?category3Id=61
        StringBuffer url = new StringBuffer();
        //  判断用户是否通过分类Id 检索
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            url.append("category3Id=").append(searchParam.getCategory3Id());
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            url.append("category2Id=").append(searchParam.getCategory2Id());
        }
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            url.append("category1Id=").append(searchParam.getCategory1Id());
        }
        //  判断用户是否通过关键词 检索  list.html?keyword=小米手机
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            url.append("keyword=").append(searchParam.getKeyword());
        }
        //  是否根据品牌检索 list.html?category3Id=61&trademark=1:小米
        if (!StringUtils.isEmpty(searchParam.getTrademark())) {
            if (url.length() > 0) {
                url.append("&trademark=").append(searchParam.getTrademark());
            }
        }
        //  是否根据平台属性检索
        //  list.html?category3Id=61&trademark=1:小米&props=23:4G:运行内存&props=24:256G:机身内存
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            //  循环遍历
            for (String prop : props) {
                if (url.length() > 0) {
                    url.append("&props=").append(prop);
                }
            }
        }
        return "list.html?" + url.toString();
    }
}
