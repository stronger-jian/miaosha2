package com.miaoshaproject.controller;

import com.miaoshaproject.controller.viewobjcet.ItemVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.CacheService;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.ItemModel;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author: crowsjian
 * @Date: 2019/11/30 22:05
 */
@Controller("item")
@RequestMapping("/item")
@CrossOrigin(allowCredentials="true", allowedHeaders="*")
public class ItemController extends BaseController {
    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private PromoService promoService;

    //商品详情
    @RequestMapping(value = "/publishpromo", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType publishpromo(@RequestParam(value = "id")Integer id){
        promoService.publishPromo(id);
        return CommonReturnType.create(null);
    }

    //商品详情
    @RequestMapping(value = "/get", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType get(@RequestParam(value = "id")Integer id){
        ItemModel itemModel = null;
        // 先取本地缓存
        itemModel = (ItemModel) cacheService.getCommonCache("item_"+id);
        if(itemModel == null){
            // 根据商品id到redis内获取
            itemModel = (ItemModel)redisTemplate.opsForValue().get("item_"+id);
            // 若redis内部存在对应的itemmodel，访问下游的service
            if(itemModel == null){
                itemModel = itemService.getItemById(id);
                // 设置itemModel到redis内
                redisTemplate.opsForValue().set("item_"+id, itemModel);
                //失效时间
                redisTemplate.expire("item_"+id, 10, TimeUnit.MINUTES);
            }
            //填充本地缓存
            cacheService.setCommonCache("item_"+id,itemModel);
        }

        ItemVO itemVO = convertFromModel(itemModel);
        return CommonReturnType.create(itemVO);
    }

    //商品列表
    @RequestMapping(value = "/list", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType listItem(){
        List<ItemModel> itemModelList = itemService.listItem();
        //使用stram api 将list内的ItemModel转化成为itemVOList
        List<ItemVO> itemVOList = itemModelList.stream().map(itemModel -> {
            ItemVO itemVO = this.convertFromModel(itemModel);
            return itemVO;
        }).collect(Collectors.toList());
        return CommonReturnType.create(itemVOList);
    }

    // 创建商品
    @RequestMapping(value = "/createItem", method = {RequestMethod.POST}, consumes = {CONTENT_TYPR_FORMED})
    @ResponseBody
    public CommonReturnType createItem(@RequestParam(value = "title")String title,
                                       @RequestParam(value = "price") BigDecimal price,
                                       @RequestParam(value = "stock")Integer stock,
                                       @RequestParam(value = "description")String description,
                                       @RequestParam(value = "sales")Integer sales,
                                       @RequestParam(value = "imgUrl")String imgUrl) throws BusinessException {
        //封装service请求用来创建商品
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setPrice(price);
        itemModel.setStock(stock);
        itemModel.setDescription(description);
        itemModel.setSales(sales);
        itemModel.setImgUrl(imgUrl);
        ItemModel itemModelResult = itemService.createItem(itemModel);
        return CommonReturnType.create(convertFromModel(itemModelResult));
    }
    private ItemVO convertFromModel(ItemModel itemModel){
        if(itemModel == null){
            return null;
        }
        ItemVO result = new ItemVO();
        BeanUtils.copyProperties(itemModel, result);
        if(itemModel.getPromoModel()!=null){
            // 表示有正在进行的秒杀活动
            result.setPromoStatus(itemModel.getPromoModel().getStatus());
            result.setPromoId(itemModel.getPromoModel().getId());
            result.setPromoPrice(itemModel.getPromoModel().getPromoPrice());
            result.setStartDate(itemModel.getPromoModel().getStartTime().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
        }else{
            result.setPromoStatus(0);
        }
        return result;
    }
}
