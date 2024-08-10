package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class SeckillController {
    @Autowired
    private ActivityFeignClient activityFeignClient;

    /**
     * 秒杀列表
     *
     * @param model
     * @return
     */
    @GetMapping("seckill.html")
    public String seckillIndex(Model model) {
        Result result = activityFeignClient.findAll();
        //
//        model.addAllAttributes()//all 存储集合 页面需要的key 都在实现类以及封装号！

        model.addAttribute("list", result.getData());
        return "seckill/index";
    }

    @GetMapping("seckill/{skuId}.html")
    public String seckillItem(@PathVariable Long skuId, Model model) {
        // 通过skuId 查询skuInfo
        Result result = activityFeignClient.getSeckillGoods(skuId);
        model.addAttribute("item", result.getData());
        return "seckill/item";
    }

    /**
     * 渲染排队页面
     * @param skuId
     * @param skuIdStr
     * @param model
     * @return
     */
    @GetMapping("seckill/queue.html")
    public String queue(@RequestParam(name = "skuId") Long skuId,
                        @RequestParam(name = "skuIdStr") String skuIdStr,
                        Model model) {
        model.addAttribute("skuId", skuId);
        model.addAttribute("skuIdStr", skuIdStr);
        return "seckill/queue";
    }


    /**
     * 返回订单结算页
     * @param model
     * @return
     */
    @GetMapping("seckill/trade.html")
    public String trade(Model model) {
        // userAddressList detailArrayList totalAmount totalNum
        Result<Map<String, Object>> result = activityFeignClient.seckillTradeData();
        if(result.isOk()){
            //保存数据到前端页面
            model.addAllAttributes(result.getData());
            //返回订单结算页
            return "seckill/trade";
        }else{
            //返回失败页面
            model.addAttribute("message", result.getMessage());
            return "seckill/fail";
        }
    }

}