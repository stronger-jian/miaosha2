package com.miaoshaproject.service.Impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.miaoshaproject.service.CacheService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @Author: crowsjian
 * @Date: 2019/12/29 13:38
 */
@Service
public class CacheServiceImpl implements CacheService {

    private Cache<String, Object> commonCache = null;

    @PostConstruct
    public void init(){
        commonCache = CacheBuilder.newBuilder().
                // 设置缓存器初始容量为10
                initialCapacity(10).
                // 设置缓存最大可以存储100个key，超过100之后会按照lru的策略移除缓存项
                maximumSize(100).
                // 过期时间多少秒
                expireAfterWrite(60, TimeUnit.SECONDS).build();
    }

    @Override
    public void setCommonCache(String key, Object value) {
        commonCache.put(key, value);
    }

    @Override
    public Object getCommonCache(String key) {
        return commonCache.getIfPresent(key);
    }
}
