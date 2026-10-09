package com.hz.web.mapper.provider;

import com.hz.constant.MyConstant;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author saber
 */
public class GeoDataProvider {
    public String list(@Param("tableName") String tableName, @Param("fields") List<String> fields, @Param("wkt") String wkt, @Param("sqlCondition") String sqlCondition, boolean geomAsBinary) {
        // 如果tableName是小写，就加""
//        if (!tableName.equals(tableName.toUpperCase())){
//            tableName = "\"" + tableName + "\"";
//        }
        fields.forEach(field -> {
            if (!field.equals(field.toUpperCase())){
                field = "\"" + field + "\"";
                fields.set(fields.indexOf(field), field);
            }
        });

        String fieldsStr = String.join(",", fields);
        String sql = geomAsBinary
                ?
                String.format("select %s, %s from %s where %s %s",
                        fieldsStr,
                        String.format("DBMS_LOB.SUBSTR(%s.ST_AsBinary(%s), %d, 1) AS %s", MyConstant.SPATIAL_PKG, MyConstant.GEOM_FIELD_NAME, MyConstant.BLOB_TO_BINARY_LEN_LIMIT, MyConstant.WKB_FIELD_NAME),
                        tableName.toUpperCase(),
                        String.format("%s < %d", MyConstant.GEOM_LEN_FIELD_NAME, MyConstant.BLOB_TO_BINARY_LEN_LIMIT),
                        wkt == null ? "" : String.format(" and %s.ST_Intersects(%s, %s.ST_GeomFromText('%s'))", MyConstant.SPATIAL_PKG, MyConstant.GEOM_FIELD_NAME, MyConstant.SPATIAL_PKG, wkt)
                )
                :
                String.format("select %s, %s from %s where %s %s",
                        fieldsStr,
                        MyConstant.SPATIAL_PKG + ".ST_AsBinary(" + MyConstant.GEOM_FIELD_NAME + ") as " + MyConstant.WKB_FIELD_NAME,
                        tableName.toUpperCase(),
                        String.format("%s >= %d", MyConstant.GEOM_LEN_FIELD_NAME, MyConstant.BLOB_TO_BINARY_LEN_LIMIT),
                        wkt == null ? "" : String.format(" and %s.ST_Intersects(%s, %s.ST_GeomFromText('%s'))", MyConstant.SPATIAL_PKG, MyConstant.GEOM_FIELD_NAME, MyConstant.SPATIAL_PKG, wkt)
                );
//        System.out.println(sql);
        if (sqlCondition != null){
            sql = sql + " and " + sqlCondition;
        }
        return sql;
    }

    public String listAttributeWithoutGeom(@Param("tableName") String tableName, @Param("fields") List<String> fields, @Param("wkt") String wkt, @Param("sqlCondition") String sqlCondition) {
        String fieldsStr = String.join(",", fields);
        String sql = String.format("select %s from %s where %s %s",
                fieldsStr,
                tableName.toUpperCase(),
                String.format("%s >= %d", MyConstant.GEOM_LEN_FIELD_NAME, MyConstant.BLOB_TO_BINARY_LEN_LIMIT),
                wkt == null ? "" : String.format(" and %s.ST_Intersects(%s, %s.ST_GeomFromText('%s'))", MyConstant.SPATIAL_PKG, MyConstant.GEOM_FIELD_NAME, MyConstant.SPATIAL_PKG, wkt)
        );
//        System.out.println(sql);
        if (sqlCondition != null){
            sql = sql + " and " + sqlCondition;
        }
        return sql;
    }

    public String listGeom(@Param("tableName") String tableName) {
        // 如果tableName是小写，就加""
//        if (!tableName.equals(tableName.toUpperCase())){
//            tableName = "\"" + tableName + "\"";
//        }
        return String.format("select %s,%s from %s where %s",
                MyConstant.ID_FIELD_NAME,
                MyConstant.SPATIAL_PKG + ".ST_AsBinary(" + MyConstant.GEOM_FIELD_NAME + ") as " + MyConstant.WKB_FIELD_NAME,
                tableName.toUpperCase(),
                String.format("%s >= %d", MyConstant.GEOM_LEN_FIELD_NAME, MyConstant.BLOB_TO_BINARY_LEN_LIMIT));
    }


//    public String listAsBlob(@Param("tableName") String tableName, @Param("fields") List<String> fields) {
//        String fields_str = String.join(",", fields);
//        return String.format("select %s, %s from %s",
//                fields_str,
//                MyConstant.SPATIAL_PKG + ".ST_AsBinary(" + MyConstant.GEOM_FIELD_NAME + ") as " + MyConstant.WKB_FIELD_NAME,
//                tableName.toUpperCase());
//    }

    // 获取除空间字段外的所有字段
    public String getFieldsWithoutGeoField(@Param("tableName") String tableName) {
        return String.format("SELECT LISTAGG(column_name, ', ') WITHIN GROUP (ORDER BY column_id) AS cols FROM user_tab_columns WHERE table_name = '%s' AND column_name <> '%s'",
                tableName.toUpperCase(),
                MyConstant.GEOM_FIELD_NAME);
    }


}
