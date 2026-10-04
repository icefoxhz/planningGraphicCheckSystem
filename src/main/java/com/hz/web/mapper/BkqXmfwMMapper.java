package com.hz.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hz.web.entity.BkqXmfwMEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 项目范围面 BKQ_XMFW_M —— 只负责数据插入。
 * <p>
 * 插入必须写成显式 SQL，不能交给 MyBatis-Plus 的自动 insert：几何列 SHAPE 是 SYSGEO.ST_GEOMETRY，
 * 传 byte[] 或 String 都塞不进去，必须用 {@code <spatialPkg>.ST_GeomFromText(wkt, srid)} 转换。
 * 这里一条语句把属性列和几何一起写入。
 * <p>
 * 属性列的查询/更新/删除仍然可以用继承来的 BaseMapper 方法（或 ServiceImpl 自带方法）。
 * <p>
 * 关于 {@code ${pkg}}：达梦的 DMGEO 与 DMGEO2 两套包不兼容，函数包前缀由配置
 * myProject.geoTable.dmgeo 决定（见 MyConstant.SPATIAL_PKG），而注解上的 SQL 必须是编译期常量，
 * 取不到运行时配置，所以用 {@code ${pkg}} 占位，由调用方（Service）传入 MyConstant.SPATIAL_PKG。
 * 该值来自配置文件而非前端入参，不存在注入风险；但请不要把用户输入传进 pkg。
 *
 * @author saber
 */
@Mapper
@Repository
public interface BkqXmfwMMapper extends BaseMapper<BkqXmfwMEntity> {
}
