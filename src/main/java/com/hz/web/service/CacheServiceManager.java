package com.hz.web.service;


import com.hz.web.service.impl.MemCacheServiceImpl;
import com.hz.web.service.impl.RedisCacheServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author saber
 */
@Service
@Slf4j
public class CacheServiceManager {
    @Autowired
    MemCacheServiceImpl memCacheService;

    @Autowired
    RedisCacheServiceImpl redisCacheService;

    @Value("${myProject.cache.strategy}")
    private String strategy;

    public AbsCacheService getCacheService() {
        switch (strategy.toLowerCase()) {
            case "redis":
                return redisCacheService;
            case "memory":
                return memCacheService;
            default:
                throw new IllegalArgumentException("未知缓存策略: " + strategy);
        }
    }
}
