package com.hz.web.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * @author saber
 */
@Mapper
@Repository
public interface
TableMapper extends BaseMapper<Object> {
    // 如果当前用户和SCHEMA不对应会有问题，使用下面的写法，指定当前SCHEMA
//    @Select("select table_name from all_tables where owner = '${owner}'")
    @Select("SELECT table_name FROM all_tables WHERE owner = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')")
    List<String> getTables(@Param("owner") String owner);

    @Update("ALTER TABLE ${tableName} ADD ${fieldName} ${fieldType}")
    void createField(@Param("tableName") String tableName, @Param("fieldName") String fieldName,  @Param("fieldType") String fieldType);

    @Update("CREATE INDEX idx_${fieldName} ON ${tableName} (${fieldName})")
    void createIndex(@Param("tableName") String tableName, @Param("fieldName") String fieldName);

    @Update("CREATE spatial INDEX ${tableName}_spidx ON ${tableName}(${fieldName})")
    void createSpatialIndex(@Param("tableName") String tableName, @Param("fieldName") String fieldName);

    @Update("update ${tableName} set ${updateFieldName}=DBMS_LOB.GETLENGTH(${geomFieldName}) where ${updateFieldName} is null")
    void updateGeomLen(@Param("tableName") String tableName, @Param("updateFieldName") String updateFieldName, @Param("geomFieldName") String geomFieldName);

    @Select("select id from gis_红线_正式红线")
    List<Map<String, Object>> getTestData();
}
