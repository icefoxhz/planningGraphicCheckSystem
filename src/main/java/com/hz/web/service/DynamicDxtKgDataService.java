package com.hz.web.service;

import com.hz.constant.MyConstant;
import com.hz.utils.ConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author saber
 */

@Service
@Slf4j
public class DynamicDxtKgDataService {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    TableMetadataService tableMetadataService;

    @Value("${myProject.geoTable.srid}")
    private int srid;

    // 业务约定：op_type 0 = 更新后原数据，1 = 新增


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

        String updateSql = String.format("update %s set %s=%d where %s=%d",
                tableName.toUpperCase(), MyConstant.GEOM_LEN_FIELD_NAME, data.get(MyConstant.GEOM_FIELD_NAME).toString().length(), MyConstant.ID_FIELD_NAME, key);
        jdbcTemplate.update(updateSql);

        return key != null ? key.longValue() : null;
    }

    /**
     * 物理删除指定表的一条记录。
     * <p>表名只能拼字符串（占位符不能用于表名），而它的来源是 HTTP 入参，所以这里必须先做白名单校验。</p>
     *
     * @param tableName 表名，只允许字母、数字、下划线
     * @param objectId  主键值（OBJECTID）；纯数字按数值绑定，其余原样按字符串比对
     * @return 实际删除的行数，0 表示没有匹配到记录
     */
    public int delete(String tableName, String objectId) {
        checkTableName(tableName);
        if (objectId == null || objectId.trim().isEmpty()) {
            throw new IllegalArgumentException("objectId 不能为空");
        }

        String sql = String.format(
                "delete from %s where %s = ?", tableName.toUpperCase(), MyConstant.ID_FIELD_NAME);

        String id = objectId.trim();
        // OBJECTID 在空间表里是 INT，按数值绑定，避免达梦对字符参数做隐式转换
        Object idParam = id.matches("\\d+") ? Long.valueOf(id) : id;

        return jdbcTemplate.update(sql, idParam);
    }

    /**
     * 表名白名单校验：表名会被直接拼进 SQL，不能信任调用方传入的值。
     */
    private void checkTableName(String tableName) {
        if (tableName == null || !tableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("非法表名：" + tableName);
        }
    }


    public boolean isOverlap(String wkt, String tableName) {
        // 只和现状的比，op_type = 1 即本次新增
        String sql = String.format("select count(1) from %s where op_type = %d and %s.ST_Intersects(%s, %s.ST_GeomFromText('%s'))", tableName.toUpperCase(), 1, MyConstant.SPATIAL_PKG, MyConstant.GEOM_FIELD_NAME, MyConstant.SPATIAL_PKG, wkt);
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
//        // 增量和范围入库都要做的回滚操作
//        String sql = String.format("delete from %s where op_type=? and op_user=? and op_time=?", tableName.toUpperCase());
//        jdbcTemplate.update(sql, NEW_ADD_OP_TYPE, userName, insertTime);
//
//        if (hadWkt) {
//            // 范围更新回滚，额外操作
//            String updateSql = String.format("update %s set op_type=?, op_reason='范围更新回滚' where op_type=? and op_user=? and op_time=?", tableName.toUpperCase());
//            jdbcTemplate.update(updateSql, NEW_ADD_OP_TYPE, AFTER_UPDATE_OP_TYPE, userName, insertTime);
//        }
    }

}
