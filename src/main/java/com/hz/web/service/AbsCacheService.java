package com.hz.web.service;

import com.hz.constant.MyConstant;
import com.hz.utils.ConvertUtil;
import com.hz.web.mapper.GeoDataMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author saber
 */
@Slf4j
public abstract class AbsCacheService {
    public void initCache(GeoDataMapper geoDataMapper) {
        AtomicInteger cacheRows = new AtomicInteger(0);
        AtomicInteger okTables = new AtomicInteger(0);
        AtomicInteger skipTables = new AtomicInteger(0);
        AtomicInteger failTables = new AtomicInteger(0);

        // 逐表隔离：单张表失败/跳过不影响其它表，且能明确日志定位到表名
        MyConstant.GEOM_TABLES.parallelStream().forEach(tableName -> {
            try {
                List<Map<String, Object>> dataList = geoDataMapper.listGeom(tableName);
                if (dataList == null || dataList.isEmpty()) {
                    skipTables.incrementAndGet();
                    log.info("表: {}, 无可预热大图形, 跳过", tableName);
                    return;
                }
                cacheRows.getAndAdd(dataList.size());
                ConvertUtil.convertBlobToBytes(tableName, dataList, null);
                dataList.parallelStream().forEach(stringObjectMap -> {
                    int id = Integer.parseInt(stringObjectMap.get(MyConstant.ID_FIELD_NAME).toString());
                    Object val = stringObjectMap.get(MyConstant.WKB_FIELD_NAME);
                    setGeomBlobToCache(tableName, id, val);
                });
                okTables.incrementAndGet();
                log.info("表: {}, 缓存条数:{}", tableName, dataList.size());
            } catch (Exception e) {
                // 常见原因：该表 GEOM 列的类型与配置的空间函数族（MyConstant.SPATIAL_PKG）不匹配、
                // 或缺少 idFieldName/GEOM_LEN 等约定字段
                failTables.incrementAndGet();
                log.warn("表: {}, 大图形缓存预热失败, 已跳过. 原因: {}", tableName, e.getMessage());
            }
        });

        log.info("大图形缓存创建完成, 成功表数: {}, 跳过表数: {}, 失败表数: {}, 缓存总数: {} 条",
                okTables.get(), skipTables.get(), failTables.get(), cacheRows.get());
    }

    public abstract void initCache();

    public abstract Object getGeomBlobFromCache(String tableName, Integer objectId) throws Exception;

    public abstract void setGeomBlobToCache(String tableName, Integer objectId, Object blobValue);
}
