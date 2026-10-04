package com.hz.web.service.impl;

import com.hz.constant.MyConstant;
import com.hz.utils.ZLibUtil;
import com.hz.web.mapper.GeoDataMapper;
import com.hz.web.service.RedisStorageService;
import com.hz.web.service.AbsCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author saber
 */

@Service
@Slf4j
public class RedisCacheServiceImpl extends AbsCacheService {
    @Autowired
    GeoDataMapper geoDataMapper;

    @Autowired
    RedisStorageService redisStorageService;

    @Value("${myProject.cache.dataCompress}")
    private boolean dataCompress;

    @Override
    public void initCache() {
        boolean hasDoInit = true;
        // 发现有1个，就说明缓存过了
        for (String geomTable : MyConstant.GEOM_TABLES) {
            Map<Object, Object> outerKeyMap = redisStorageService.getOuterKeyMap(geomTable);
            if (!outerKeyMap.isEmpty()) {
                hasDoInit = false;
                break;
            }
        }

        if (hasDoInit) {
            log.info("开始初始化大图形缓存......");
            super.initCache(geoDataMapper);
        }else{
            log.info("大图形缓存已经存在！");
        }
    }

    @Override
    public Object getGeomBlobFromCache(String tableName, Integer objectId) {
        Object geomBytes = redisStorageService.getGeomBytes(tableName, objectId);
        return geomBytes == null ? null : (dataCompress ? ZLibUtil.decompress((byte[]) geomBytes) : geomBytes);
    }

    @Override
    public void setGeomBlobToCache(String tableName, Integer objectId, Object blobValue) {
        byte[] compressedBlobValue = dataCompress ? ZLibUtil.compress((byte[]) blobValue) : (byte[])blobValue;
        redisStorageService.setGeomBytes(tableName, objectId, compressedBlobValue);
    }
}
