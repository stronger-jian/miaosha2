package com.miaoshaproject.service.Impl;

import com.miaoshaproject.dao.ItemDOMapper;
import com.miaoshaproject.dao.ItemStockDOMapper;
import com.miaoshaproject.dao.StockLogDOMapper;
import com.miaoshaproject.dataobject.ItemDO;
import com.miaoshaproject.dataobject.ItemStockDO;
import com.miaoshaproject.dataobject.StockLogDO;
import com.miaoshaproject.dataobject.UserDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EMBusinessError;
import com.miaoshaproject.mq.MqProducer;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.PromoModel;
import com.miaoshaproject.validator.ValidationResult;
import com.miaoshaproject.validator.ValidatorImpl;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author: crowsjian
 * @Date: 2019/11/30 17:03
 */
@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private ItemDOMapper itemDOMapper;

    @Autowired
    private ItemStockDOMapper itemStockDOMapper;

    @Autowired
    private PromoService promoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    //初始化对应的库存流水
    @Override
    @Transactional
    public String initStockLog(Integer itemId, Integer amount) {
        StockLogDO stockLogDO = new StockLogDO();
        stockLogDO.setItemId(itemId);
        stockLogDO.setAmount(amount);
        stockLogDO.setStockLogId(UUID.randomUUID().toString().replace("-", ""));
        stockLogDO.setStatus(1);
        stockLogDOMapper.insertSelective(stockLogDO);
        return stockLogDO.getStockLogId();
    }

    @Override
    @Transactional
    public boolean deacreaseStock(Integer itemId, Integer amount) throws BusinessException {
        // int affectedRow = itemStockDOMapper.deacreaseStock(itemId, amount);
        long result =redisTemplate.opsForValue().increment("promo_item_stock_"+itemId, amount.intValue()*-1);
        if(result>0){
            // 更新成功
//            boolean mqresult = mqProducer.asyncReduceStock(itemId, amount);
//            if(!mqresult){
//                //返回失败 把库存 加回去
//                redisTemplate.opsForValue().increment("promo_item_stock_"+itemId, amount.intValue());
//                return false;
//            }
            return true;
        }else if(result==0){
            // 打上库存已售罄的标识
            redisTemplate.opsForValue().set("promo_item_stock_invalid_"+itemId,"true");
            // 更新库存成功
            return true;
        }else{
            increaseStock(itemId, amount);
            return false;
        }
    }

    @Override
    public boolean increaseStock(Integer itemId, Integer acount) {
        redisTemplate.opsForValue().increment("promo_item_stock_"+itemId, acount.intValue());
        return true;
    }

    @Override
    public boolean asyncDecreaseStock(Integer itemId, Integer acount) {
        boolean mqresult = mqProducer.asyncReduceStock(itemId, acount);
        return mqresult;
    }

    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer amount) throws BusinessException {
        itemDOMapper.increaseSales(itemId, amount);
    }

    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        // 校验入参
        ValidationResult validationResult = validator.validate(itemModel);
        if(validationResult.isHasErrors()){
            throw new BusinessException(EMBusinessError.PARAMATER_VALIDATION_ERROR, validationResult.getErrorMsg());
        }
        // 转化itemModel->dataobject
        ItemDO itemDO = convertFromModel(itemModel);
        // 写入数据库
        itemDOMapper.insertSelective(itemDO);
        itemModel.setId(itemDO.getId());
        ItemStockDO itemStockDO = convertItemStockFromModel(itemModel);
        itemStockDOMapper.insertSelective(itemStockDO);
        // 返回创建完成的对象
        return this.getItemById(itemModel.getId());
    }

    @Override
    public List<ItemModel> listItem() {
        List<ItemDO> itemDOList = itemDOMapper.listItem();
        List<ItemModel> itemModelList = itemDOList.stream().map(itemDO -> {
            ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
            ItemModel itemModel = convertFromDO(itemDO, itemStockDO);
            return itemModel;
        }).collect(Collectors.toList());
        return itemModelList;
    }

    @Override
    public ItemModel getItemById(Integer id) {
        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(id);
        if(itemDO == null){
            return null;
        }
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
        ItemModel itemModel = convertFromDO(itemDO, itemStockDO);
        // 获取活动商品信息
        PromoModel promoModel = promoService.getPromoByItemId(itemModel.getId());
        if(promoModel != null && promoModel.getStatus().intValue()!=3){
            itemModel.setPromoModel(promoModel);
        }
        return itemModel;
    }

    @Override
    public ItemModel getItemByIdInCache(Integer id) {
        ItemModel itemModel = (ItemModel)redisTemplate.opsForValue().get("item_validate_"+id);
        if(itemModel==null){
            itemModel = this.getItemById(id);
            redisTemplate.opsForValue().set("item_validate_"+id, itemModel);
            redisTemplate.expire("item_validate_"+id, 10, TimeUnit.MINUTES);
        }
        return itemModel;
    }

    private ItemModel convertFromDO(ItemDO itemDO, ItemStockDO itemStockDO){
        if(itemDO == null){
            return null;
        }
        ItemModel result = new ItemModel();
        BeanUtils.copyProperties(itemDO, result);
        result.setStock(itemStockDO.getStock());
        return result;
    }

    // 转化itemModel->dataobject
    private ItemDO convertFromModel(ItemModel itemModel){
        if(itemModel == null){
            return null;
        }
        ItemDO result = new ItemDO();
        BeanUtils.copyProperties(itemModel, result);
        return result;
    }

    private ItemStockDO convertItemStockFromModel(ItemModel itemModel){
        if(itemModel ==null){
            return null;
        }
        ItemStockDO result = new ItemStockDO();
        result.setItemId(itemModel.getId());
        result.setStock(itemModel.getStock());
        return result;
    }
}
