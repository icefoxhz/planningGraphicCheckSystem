package com.hz.web.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hz.web.entity.vo.GeoDataEntity;

import java.util.List;
import java.util.Map;

/**
 * @author saber
 */
public interface IGeoDataService extends IService<GeoDataEntity> {
    IPage<Map<String, Object>> listPage(String tableName, int page, int size);

    List<Map<String, Object>> listLittleGeom(String tableName, String wkt, String sqlCondition);

    List<Map<String, Object>> listAttributeWithoutGeom(String tableName, String wkt, String sqlCondition);

//    void listToRedis(String queueKey, String tableName);
//
//    void pushStopFlagToRedis(String queueKey, String error);
}
