package com.miaoshaproject.service;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.model.PromoModel;

/**
 * @Author: crowsjian
 * @Date: 2019/12/7 10:04
 */
public interface PromoService {
    // 根据itemId 获取正在进行的或即将进行的秒杀活动
    PromoModel getPromoByItemId(Integer itemId);
    // 发布活动
    void publishPromo(Integer pormoId);
    // 生成秒杀用的令牌
    String generateSecondKillToken(Integer pormoId, Integer itemId, Integer userId) throws BusinessException;
}
