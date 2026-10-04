package com.hz.web.mapper;

import com.hz.web.entity.LayerFieldsEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author saber
 * @since 2025-05-14
 */

@Mapper
@Repository
public interface LayerFieldsMapper extends BaseMapper<LayerFieldsEntity> {
    @Select("select count(1) from LAYERFIELDS where UPPER(FIELD_NAME_EN) = UPPER(#{fieldNameEn}) and LAYER_ID = #{layerId}")
    Integer isFieldNameExist(@Param("layerId") Integer layerId, @Param("fieldNameEn") String fieldNameEn);

    @Select("select max(#{fieldNameEn}) from LAYERFIELDS where LAYER_ID = #{layerId}")
    String getFieldValueByRule(@Param("layerId") Integer layerId, @Param("fieldNameEn") String fieldNameEn);

    @Select("SELECT a.* FROM LAYERFIELDS a JOIN ROLE_LAYERFIELDS_PERMISSIONS b ON a.LAYER_ID = b.LAYER_ID AND a.id = b.FIELD_ID WHERE b.ROLE_ID = #{roleId} AND b.LAYER_ID = #{layerId}")
    List<LayerFieldsEntity> getFieldsByRole(@Param("roleId") Integer roleId, @Param("layerId") Integer layerId);
}
