package com.miaoshaproject.service.Impl;

import com.miaoshaproject.dao.PromoDOMapper;
import com.miaoshaproject.dataobject.PromoDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EMBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.PromoModel;
import com.miaoshaproject.service.model.UserModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author: crowsjian
 * @Date: 2019/12/7 10:06
 */
@Service
public class PromoServiceImpl implements PromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @Override
    public String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId) throws BusinessException {

        // 判断时否库存售罄，若对应售罄的key存在，则直接返回下单失败
        if(redisTemplate.hasKey("promo_item_stock_invalid_"+itemId)){
            throw new  BusinessException(EMBusinessError.STOCK_NOT_ENOUGH);
        };

        // 获取秒杀商品详细信息
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        PromoModel promoModel = convertFromDO(promoDO);
        if(promoModel == null){
            return null;
        }
        // 判断当前时间秒杀活动是否正在进行或即将开始
        DateTime now = new DateTime();
        if(promoModel.getStartTime().isAfter(now)){
            promoModel.setStatus(1); // 还未开始
        }else if(promoModel.getEndTime().isBefore(now)){
            promoModel.setStatus(3);// 已近结束
        }else{
            promoModel.setStatus(2);// 正在进行
        }
        if(promoModel.getStatus().intValue()!=2){
            return null;
        }
        // 判断item信息
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if(itemModel == null){
            return null;
        }
        // 判断用户信息
        UserModel userModel = userService.getUserByIdInCache(userId);
        if(userModel == null){
            return null;
        }
        // 获取秒杀大闸 count 的数量
        long  result = redisTemplate.opsForValue().increment("promo_door_count_"+promoId, -1);
        if(result<0){
            return null;
        }
        // 生成token
        String token = UUID.randomUUID().toString().replace("-","");
        redisTemplate.opsForValue().set("promo_token_"+promoId+"userId_"+userId+"itemId_"+itemId, token);
        //设置  过期时间  五分钟
        redisTemplate.expire("promo_token_"+promoId+"userId_"+userId+"itemId_"+itemId, 5, TimeUnit.MINUTES);
        return token;
    }

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        // 获取秒杀商品详细信息
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);
        // dataobject=>model
        PromoModel promoModel = convertFromDO(promoDO);
        if(promoModel == null){
            return null;
        }
        // 判断当前时间秒杀活动是否正在进行或即将开始
        DateTime now = new DateTime();
        if(promoModel.getStartTime().isAfter(now)){
            promoModel.setStatus(1); // 还未开始
        }else if(promoModel.getEndTime().isBefore(now)){
            promoModel.setStatus(3);// 已近结束
        }else{
            promoModel.setStatus(2);// 正在进行
        }
        return promoModel;
    }

    @Override
    public void publishPromo(Integer promoId) {
        // 通过活动id 获取活动
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId) ;
        if(promoDO == null|| promoDO.getItemId().intValue()==0){
            return;
        }
        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());
        // 将库存 同步 到 redis内
        redisTemplate.opsForValue().set("promo_item_stock_"+itemModel.getId(), itemModel.getStock());
        //将大闸的限制数字设到 redis内
        redisTemplate.opsForValue().set("promo_door_count_"+promoId, itemModel.getStock().intValue()*5);
    }

    private PromoModel convertFromDO(PromoDO promoDO){
        if(promoDO == null){
           return null;
        }
        PromoModel result = new PromoModel();
        BeanUtils.copyProperties(promoDO, result);
        result.setStartTime(new DateTime(promoDO.getStartTime()));
        result.setEndTime(new DateTime(promoDO.getEndTime()));
        result.setPromoPrice(promoDO.getPromoItemPrice());
        return result;
    }
}
