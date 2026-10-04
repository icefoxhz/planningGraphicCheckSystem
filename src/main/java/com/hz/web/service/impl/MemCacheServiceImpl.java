package com.hz.web.service.impl;

import com.hz.constant.MyConstant;
import com.hz.utils.ZLibUtil;
import com.hz.web.mapper.GeoDataMapper;
import com.hz.web.service.AbsCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author saber
 */

@Service
@Slf4j
public class MemCacheServiceImpl extends AbsCacheService {
    @Autowired
    GeoDataMapper geoDataMapper;

    @Value("${myProject.cache.dataCompress}")
    private boolean dataCompress;

    @Override
    public void initCache() {
       super.initCache(geoDataMapper);
    }

    @Override
    public Object getGeomBlobFromCache(String tableName, Integer objectId) {
        ConcurrentHashMap<Integer, Object> memCache;
        if (!MyConstant.LAYER_ID_GEOM_BIG_BINARY_CACHE.containsKey(tableName)) {
            memCache = new ConcurrentHashMap<>();
            MyConstant.LAYER_ID_GEOM_BIG_BINARY_CACHE.put(tableName, memCache);
        } else {
            memCache = MyConstant.LAYER_ID_GEOM_BIG_BINARY_CACHE.get(tableName);
        }

//        if (memCache.containsKey(objectId)) {
//            log.info("缓存命中,table: {}, id: {}", tableName, objectId);
//        }

        Object val = memCache.get(objectId);
        if (val != null){
            return dataCompress ? ZLibUtil.decompress((byte[])val) : val;
        }
        return null;
    }

    @Override
    public void setGeomBlobToCache(String tableName, Integer objectId, Object blobValue) {
        ConcurrentHashMap<Integer, Object> memCache = MyConstant.LAYER_ID_GEOM_BIG_BINARY_CACHE.get(tableName);
        if (memCache == null) {
            memCache = new ConcurrentHashMap<>();
            MyConstant.LAYER_ID_GEOM_BIG_BINARY_CACHE.put(tableName, memCache);
        }

        memCache.put(objectId, dataCompress ? ZLibUtil.compress((byte[]) blobValue) : blobValue);
    }
}
