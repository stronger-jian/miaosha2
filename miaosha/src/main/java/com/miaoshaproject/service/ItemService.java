package com.miaoshaproject.service;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.model.ItemModel;

import java.util.List;

/**
 * 商品
 * @Author: crowsjian
 * @Date: 2019/11/30 16:16
 */
public interface ItemService {
    // 创建商品
    ItemModel createItem(ItemModel itemModel) throws BusinessException;
    // 商品列表
    List<ItemModel>  listItem();
    //商品详情
    ItemModel getItemById(Integer id);
    //item及promo model缓存模型
    ItemModel getItemByIdInCache(Integer id);
    // 库存回补
    boolean increaseStock(Integer itemId, Integer acount) ;
    // 商品减库存
    boolean deacreaseStock(Integer itemId, Integer acount) throws BusinessException;
    // 一部商品减库存
    boolean asyncDecreaseStock(Integer itemId, Integer acount);
    // 销量增加
    void increaseSales(Integer itemId, Integer acount) throws BusinessException;
    // 初始化库存流水
    String initStockLog(Integer itemId, Integer amount);
}
