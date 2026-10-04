package com.hz.web.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hz.constant.MyConstant;
import com.hz.web.entity.vo.GeoDataEntity;
import com.hz.web.mapper.provider.GeoDataProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author saber
 */
@Mapper
@Repository
public interface GeoDataMapper extends BaseMapper<GeoDataEntity> {
    @SelectProvider(type = GeoDataProvider.class, method = "list")
    IPage<Map<String, Object>> listPage(IPage<Object> pages, @Param("tableName") String tableName, @Param("fields") List<String> fields);

    @SelectProvider(type = GeoDataProvider.class, method = "list")
    List<Map<String, Object>> list(@Param("tableName") String tableName, @Param("fields") List<String> fields, @Param("wkt") String wkt, @Param("sqlCondition") String sqlCondition, boolean geomAsBinary);

    @SelectProvider(type = GeoDataProvider.class, method = "listGeom")
    List<Map<String, Object>> listGeom(@Param("tableName") String tableName);

    @SelectProvider(type = GeoDataProvider.class, method = "listAttributeWithoutGeom")
    List<Map<String, Object>> listAttributeWithoutGeom(@Param("tableName") String tableName, @Param("fields") List<String> fields, @Param("wkt") String wkt, @Param("sqlCondition") String sqlCondition);

//    @SelectProvider(type = GeoDataProvider.class, method = "listAsBlob")
//    List<Map<String, Object>> listAsBlob(@Param("tableName") String tableName, @Param("fields") List<String> fields);

    @SelectProvider(type = GeoDataProvider.class, method = "getFieldsWithoutGeoField")
    List<String> getFieldsWithoutGeoField(@Param("tableName") String tableName);
    
    @Select("select ${fields_str} from ${tableName} where ${id_field} = '${id_val}'")
    Map<String, Object> getDataById(@Param("fields_str") String fields_str, @Param("tableName") String tableName, @Param("id_field") String id_field, @Param("id_val") Integer id_val);
}
