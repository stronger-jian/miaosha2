package com.miaoshaproject.service;

import com.miaoshaproject.dao.OrderInfoDOMapper;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.model.OrderModel;

/**
 * @Author: crowsjian
 * @Date: 2019/12/2 22:21
 */
public interface OrderService {
    OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) throws BusinessException;
}
