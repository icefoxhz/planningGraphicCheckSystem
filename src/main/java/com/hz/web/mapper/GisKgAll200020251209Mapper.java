package com.hz.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hz.web.entity.GisKgAll200020251209Entity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 控规全要素 GIS_KG_ALL_2000_20251209
 * <p>
 * 属性列的增删改查直接使用 BaseMapper 自带方法（几何列 GEOM、备份列 GEOM_BACKUP 已在实体上标记为不参与查询）；
 * 涉及几何的查询/写入用下面的显式 SQL。
 * <p>
 * 关于 {@code ${pkg}}：达梦的 DMGEO 与 DMGEO2 两套包不兼容，函数包前缀由配置
 * myProject.geoTable.dmgeo 决定（见 MyConstant.SPATIAL_PKG），而注解上的 SQL 必须是编译期常量，
 * 取不到运行时配置，所以用 {@code ${pkg}} 占位，由调用方（service）传入 MyConstant.SPATIAL_PKG。
 * 该值来自配置文件而非前端入参，不存在注入风险；但请不要把用户输入传进 pkg。
 *
 * @author saber
 */
@Mapper
@Repository
public interface GisKgAll200020251209Mapper extends BaseMapper<GisKgAll200020251209Entity> {
}
