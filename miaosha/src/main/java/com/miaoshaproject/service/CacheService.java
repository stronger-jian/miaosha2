package com.miaoshaproject.service;

/**
 * @Author: crowsjian
 * @Date: 2019/12/29 13:36
 */
//封装本地缓存操作类
public interface CacheService {
    // 存方法
    void setCommonCache(String key, Object value);
    // 取方法
    Object getCommonCache(String key);
}
