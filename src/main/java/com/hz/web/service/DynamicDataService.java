package com.hz.web.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hz.constant.MyConstant;
import com.hz.utils.ConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * @author saber
 */

@Service
@Slf4j
public class DynamicDataService {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    TableMetadataService tableMetadataService;

    @Value("${myProject.geoTable.srid}")
    private int srid;

    // 更新后，原数据的类型
    final int AFTER_UPDATE_OP_TYPE = 0;

    // 新增
    final int NEW_ADD_OP_TYPE = 1;


//    ConcurrentHashMap<String, ConcurrentLinkedQueue<List<Map<String, Object>>>> tableDataQueue = new ConcurrentHashMap<>();
//    ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> tableInsertDataInfoMap = new ConcurrentHashMap<>();

//    /**
//     * 字段类型映射（你可以根据需要扩展）
//     */
//    private String getSqlType(Object value) {
//        if (value instanceof Integer) return "INT";
//        if (value instanceof Long) return "BIGINT";
//        if (value instanceof Double || value instanceof Float) return "DOUBLE";
//        if (value instanceof java.util.Date) return "TIMESTAMP";
//        return "VARCHAR(255)";
//    }
//
//    public void createTable(String tableName, Map<String, Object> firstRow) {
//        boolean createIndex = false;
//        // 构建建表语句
//        StringBuilder createSql = new StringBuilder("CREATE TABLE " + tableName + " (");
//        for (Map.Entry<String, Object> entry : firstRow.entrySet()) {
//            String columnName = entry.getKey();
//            Object value = entry.getValue();
//            String fieldType = MyConstant.GEOM_FIELD_NAME.equalsIgnoreCase(columnName) ? MyConstant.getGeomTypeOwner() + ".ST_Geometry" : getSqlType(value);
//            fieldType = fieldType + (MyConstant.ID_FIELD_NAME.equalsIgnoreCase(columnName) ? " PRIMARY KEY" : "");
//
//
//            createSql.append(columnName)
//                    .append(" ")
//                    .append(fieldType)
//                    .append(",");
//            if (MyConstant.ID_FIELD_NAME.equalsIgnoreCase(columnName)) {
//                createIndex = true;
//            }
//        }
//        createSql.setLength(createSql.length() - 1);
//        createSql.append(")");
//        jdbcTemplate.execute(createSql.toString());
//        // 建索引
//        if (createIndex) {
//            String sqlIdx = String.format("CREATE INDEX idx_%s_%s ON %s(%s)", tableName, MyConstant.ID_FIELD_NAME, tableName, MyConstant.ID_FIELD_NAME);
//            jdbcTemplate.execute(sqlIdx);
//        }
//    }

    public Long insertAndReturnId(String tableName, Map<String, Object> rawData) {
        if (rawData == null || rawData.isEmpty()) {
            return null;
        }

        Map<String, Object> data = ConvertUtil.convertKeysToUpper(rawData);

        // 删除 OBJECTID
        data.remove(MyConstant.ID_FIELD_NAME);

        List<String> columns = new ArrayList<>(data.keySet());

        // 构建字段和占位符
        String columnSql = columns.stream()
                .map(String::toUpperCase)
                .collect(Collectors.joining(", "));

        String placeholderSql = columns.stream()
                .map(col -> col.equalsIgnoreCase(MyConstant.GEOM_FIELD_NAME)
                        ? (srid > 0 ? MyConstant.SPATIAL_PKG + ".ST_GeomFromText(?, " + srid + ")" : MyConstant.SPATIAL_PKG + ".ST_GeomFromText(?)")
                        : "?")
                .collect(Collectors.joining(", "));

        Object[] params = columns.stream().map(data::get).toArray();

        String sql = String.format("insert into %s(%s) values(%s)", tableName.toUpperCase(), columnSql, placeholderSql);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{MyConstant.ID_FIELD_NAME});
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        }, keyHolder);

        // 获取自增主键（OBJECTID）
        Number key = keyHolder.getKey();

        String updateSql = String.format("update %s set %s=%d, op_type=%d where %s=%d",
                tableName.toUpperCase(), MyConstant.GEOM_LEN_FIELD_NAME, data.get(MyConstant.GEOM_FIELD_NAME).toString().length(), NEW_ADD_OP_TYPE, MyConstant.ID_FIELD_NAME, key);
        jdbcTemplate.update(updateSql);

        return key != null ? key.longValue() : null;
    }

    public void delete(String userName, String tableName, String reason, Integer op, String objectId) {
        // 拼接 SQL 中只能拼接表名、列名，不能拼接值
        String now = LocalDateTime.now().format(formatter);
        String sql = String.format(
                "UPDATE %s SET OP_TYPE = ?, OP_TIME = ?, OP_USER = ?, OP_REASON = ? WHERE %s = ?", tableName.toUpperCase(), MyConstant.ID_FIELD_NAME);

        jdbcTemplate.update(sql, op, now, userName, reason, objectId);
    }

    public void updateEntityHistory(String userName, String tableName, String wkt, String tm) {
        String sql = String.format("update %s set op_type = ?, op_time = ?, op_reason='范围更新', OP_USER=? where %s.ST_contains(%s, %s.ST_GeomFromText(?))", tableName.toUpperCase(), MyConstant.SPATIAL_PKG, MyConstant.GEOM_FIELD_NAME, MyConstant.SPATIAL_PKG);
        jdbcTemplate.update(sql, AFTER_UPDATE_OP_TYPE, tm, userName, wkt);
    }

    public boolean isOverlap(String wkt, String tableName) {
        // 只和现状的比
        String sql = String.format("select count(1) from %s where op_type = %d and %s.ST_Intersects(%s, %s.ST_GeomFromText('%s'))", tableName.toUpperCase(), NEW_ADD_OP_TYPE, MyConstant.SPATIAL_PKG, MyConstant.GEOM_FIELD_NAME, MyConstant.SPATIAL_PKG, wkt);
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count > 0;
    }
    public ArrayList<String> isOverlapEx(Map<String, String> wktDict, String tableName) {
        ArrayList<String> overlapIds = new ArrayList<>();
        // 只和现状的比
        for (Map.Entry<String, String> entry : wktDict.entrySet()) {
            String id = entry.getKey();
            String wkt = entry.getValue();

            if (isOverlap(wkt, tableName)) {
                overlapIds.add(id);
            }
        }

        return overlapIds;
    }

    //    @Async
    @Transactional
    public void batchInsert(String tableName, List<Map<String, Object>> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        // OBJECTID值是自动生成的, 如果有要删除
        for (Map<String, Object> stringObjectMap : dataList) {
            stringObjectMap.remove(MyConstant.ID_FIELD_NAME);
        }

        List<String> columns = new ArrayList<>(dataList.get(0).keySet());

        // 不加引号，全部转为大写（达梦默认行为）
        String columnSql = columns.stream()
                .map(String::toUpperCase)
                .collect(Collectors.joining(", "));

        String placeholderSql = columns.stream()
                .map(col -> col.equalsIgnoreCase(MyConstant.GEOM_FIELD_NAME)
                        ? (srid > 0 ? MyConstant.SPATIAL_PKG + ".ST_GeomFromText(?, " + srid + ")" : MyConstant.SPATIAL_PKG + ".ST_GeomFromText(?)")
                        : "?")
                .collect(Collectors.joining(", "));

        List<Object[]> batchParams = new ArrayList<>();
        for (Map<String, Object> row : dataList) {
            Object[] params = columns.stream().map(row::get).toArray();
            batchParams.add(params);
        }

        // SQL 示例：INSERT INTO GIS_TABLE (ID, NAME, GEOM) VALUES (?, ?, <spatialPkg>.ST_GeomFromText(?, 4326))
        String sql = String.format("insert into %s(%s) values(%s)", tableName.toUpperCase(), columnSql, placeholderSql);
        jdbcTemplate.batchUpdate(sql, batchParams);
    }

    public void rollback(String userName, String tableName, String insertTime, boolean hadWkt) {
        // 增量和范围入库都要做的回滚操作
        String sql = String.format("delete from %s where op_type=? and op_user=? and op_time=?", tableName.toUpperCase());
        jdbcTemplate.update(sql, NEW_ADD_OP_TYPE, userName, insertTime);

        if (hadWkt) {
            // 范围更新回滚，额外操作
            String updateSql = String.format("update %s set op_type=?, op_reason='范围更新回滚' where op_type=? and op_user=? and op_time=?", tableName.toUpperCase());
            jdbcTemplate.update(updateSql, NEW_ADD_OP_TYPE, AFTER_UPDATE_OP_TYPE, userName, insertTime);
        }

    }

//    private void checkTableAndColumns(String tableName, List<String> columns) {
//        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
//            throw new IllegalArgumentException("非法表名：" + tableName);
//        }
//        for (String col : columns) {
//            if (!col.matches("^[a-zA-Z0-9_]+$")) {
//                throw new IllegalArgumentException("非法字段名：" + col);
//            }
//        }
//    }
//
//    public void putDataToQueue(String tableName, List<Map<String, Object>> dataList) {
//        tableDataQueue.computeIfAbsent(tableName, k -> new ConcurrentLinkedQueue<>()).add(dataList);
//    }
//
//    @Async
//    public void insertDataFromQueue(String tableName, int totalCount) {
//        ConcurrentHashMap<String, Object> infoMap;
//        try {
//            if (!tableInsertDataInfoMap.containsKey(tableName)) {
//                infoMap = new ConcurrentHashMap<>();
//                tableInsertDataInfoMap.put(tableName, infoMap);
//            } else {
//                infoMap = tableInsertDataInfoMap.get(tableName);
//            }
//            infoMap.put("count", 0);
//            infoMap.put("error", "");
//
//
//            ConcurrentLinkedQueue<List<Map<String, Object>>> q = tableDataQueue.computeIfAbsent(tableName, k -> new ConcurrentLinkedQueue<>());
//            while (true) {
//                List<Map<String, Object>> dataList = q.poll();
//                if (dataList == null) {
//                    Thread.sleep(200);
//                    continue;
//                }
//
//                batchInsert(tableName, dataList);
//
//                int prevCount = Integer.parseInt(infoMap.get("count").toString());
//                int currentCount = dataList.size() + prevCount;
//                infoMap.put("count", currentCount);
//
//                if (currentCount >= totalCount) {
//                    log.info("本次入库完成, 总数: {}", currentCount);
//                    break;
//                }
//            }
//
//            // 更新 geom_len
//            tableMetadataService.initTable(tableName);
//        } catch (Exception e) {
//            log.error(e.toString());
//            tableInsertDataInfoMap.get(tableName).put("error", e.toString());
//        }
//    }
//
//    public Map<String, Object> getInsertInfo(String tableName) {
//        if (tableInsertDataInfoMap.containsKey(tableName)) {
//            return tableInsertDataInfoMap.get(tableName);
//        }
//        return null;
//    }

}
