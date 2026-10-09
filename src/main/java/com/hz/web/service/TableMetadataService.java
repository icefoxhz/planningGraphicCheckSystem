package com.hz.web.service;

import com.hz.constant.MyConstant;
import com.hz.web.entity.vo.JdbcFieldTypeEntity;
import com.hz.web.mapper.TableMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.hz.constant.MyConstant.GEOM_FIELD_NAME;
import static com.hz.constant.MyConstant.GEOM_LEN_FIELD_NAME;

/**
 * @author saber
 */
@Service
@Slf4j
public class TableMetadataService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TableMapper tableMapper;

    @Value("${spring.datasource.username}")
    private String username;

    public List<JdbcFieldTypeEntity> getTableFieldsInfo(String tableName) {
        List<JdbcFieldTypeEntity> jdbcFieldTypeEntities = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, tableName, null);

            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String dataType = columns.getString("TYPE_NAME");
                int columnSize = columns.getInt("COLUMN_SIZE");
                String nullable = columns.getString("IS_NULLABLE");
                jdbcFieldTypeEntities.add(
                        new JdbcFieldTypeEntity()
                                .setFieldName(columnName)
                                .setFieldType(dataType)
                                .setFieldLen(columnSize)
                                .setNullAble(nullable)
                );

//                System.out.printf("字段: %s, 类型: %s, 长度: %d, 可空: %s%n", columnName, dataType, columnSize, nullable);
            }
            columns.close();
        } catch (SQLException e) {
            log.error(e.toString());
        }
        return jdbcFieldTypeEntities;
    }

    public void createGeomLenFieldAndIdx(){
        List<String> tables = tableMapper.getTables(username.toUpperCase());
        for (String table : tables) {
            if (!table.equals(table.toLowerCase()))
                continue;
            List<JdbcFieldTypeEntity> tableFieldsInfo = getTableFieldsInfo(table);
            for (JdbcFieldTypeEntity jdbcFieldTypeEntity : tableFieldsInfo) {
                if (jdbcFieldTypeEntity.getFieldName().equalsIgnoreCase(GEOM_FIELD_NAME)){
                    MyConstant.GEOM_TABLES.add(table);
                    break;
                }
            }
        }

        System.out.println(MyConstant.GEOM_TABLES);

        MyConstant.GEOM_TABLES.parallelStream().forEach(this::initTable);

    }

    public void initTable(String table) {
        try {
            tableMapper.createField(table, GEOM_LEN_FIELD_NAME, "int");
        }catch (Exception ignored){}

        try {
            tableMapper.createIndex(table, GEOM_LEN_FIELD_NAME);
        }catch (Exception ignored){}

        try {
            tableMapper.updateGeomLen(table, GEOM_LEN_FIELD_NAME, GEOM_FIELD_NAME);
        }catch (Exception ignored){}

        try {
            tableMapper.createSpatialIndex(table, GEOM_FIELD_NAME);
        }catch (Exception ignored){}
    }
}
