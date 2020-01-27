package com.miaoshaproject.service;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.model.UserModel;
import org.springframework.stereotype.Component;

/**
 * @Author: crowsjian
 * @Date: 2019/11/24 8:35
 */
public interface UserService {
    // 通过用户id获取用户对象的方法
    UserModel getUserById(Integer id);

    // 通过缓存获取user对象
    UserModel getUserByIdInCache(Integer id);
    // 注册
    void register(UserModel userModel) throws BusinessException;

    /**
     * 登录
     * @param telphone 用户注册手机号
     * @param encrptPassword 用户加密后的密码
     * @throws BusinessException
     */
    UserModel login(String telphone, String encrptPassword) throws BusinessException;
}
