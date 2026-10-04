package com.hz.web.service.impl;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hz.web.entity.vo.GeoDataEntity;
import com.hz.web.mapper.GeoDataMapper;
import com.hz.web.service.IGeoDataService;
//import com.hz.web.service.RedisQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author saber
 */
@Service
public class GeoDataServiceImpl extends ServiceImpl<GeoDataMapper, GeoDataEntity> implements IGeoDataService {
    @Autowired
    GeoDataMapper geoDataMapper;

//    @Autowired
//    RedisQueueService redisQueueService;

    @Override
    public IPage<Map<String, Object>> listPage(String tableName, int page, int size) {
        List<String> fieldsWithoutGeoField = geoDataMapper.getFieldsWithoutGeoField(tableName);
        return geoDataMapper.listPage(new Page<>(page, size), tableName, fieldsWithoutGeoField);
    }

    @Override
    public List<Map<String, Object>> listLittleGeom(String tableName, String wkt, String sqlCondition) {
        List<String> fieldsWithoutGeoField = geoDataMapper.getFieldsWithoutGeoField(tableName);
        return geoDataMapper.list(tableName, fieldsWithoutGeoField, wkt, sqlCondition, true);
    }

    @Override
    public List<Map<String, Object>> listAttributeWithoutGeom(String tableName, String wkt, String sqlCondition) {
        List<String> fieldsWithoutGeoField = geoDataMapper.getFieldsWithoutGeoField(tableName);
        return geoDataMapper.listAttributeWithoutGeom(tableName, fieldsWithoutGeoField, wkt, sqlCondition);
    }


//    @Override
//    @Async
//    public void listToRedis(String queueKey, String tableName) {
//        try {
//            // clear first
//            redisQueueService.clearQueue(queueKey);
//
//            List<Map<String, Object>> list = geoDataMapper.list(tableName);
//            list.forEach(stringObjectMap -> {
//                int id = Integer.parseInt(stringObjectMap.get(MyConstant.idFieldName).toString());
//                String wkt = stringObjectMap.get(MyConstant.wktFieldName).toString();
//                try {
//                    JSONObject jsonObject = GeomCovertUtil.wkt2JsonObject(wkt);
//                    jsonObject.put(MyConstant.idFieldName, id);
//
//                    redisQueueService.pushMessage(queueKey, jsonObject.toJSONString());
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            });
//            pushStopFlagToRedis(queueKey, "");
//        }catch (Exception e){
//            pushStopFlagToRedis(queueKey, e.toString());
//        }
//    }
//
//    @Override
//    public void pushStopFlagToRedis(String queueKey, String error) {
//        JSONObject jsonObject = new JSONObject();
//        // 结束标记位 id = -1
//        jsonObject.put(MyConstant.idFieldName, -1);
//        jsonObject.put("msg", error);
//        redisQueueService.pushMessage(queueKey, jsonObject.toJSONString());
//    }
}
