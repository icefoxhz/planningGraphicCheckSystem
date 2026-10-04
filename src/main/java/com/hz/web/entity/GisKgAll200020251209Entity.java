package com.hz.web.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.ibatis.type.JdbcType;

import java.io.Serializable;

/**
 * 控规全要素 GIS_KG_ALL_2000_20251209
 * <p>
 * 几何列名是 GEOM（与 yml 里 geoFieldName 一致），类型为 SYSGEO.ST_GEOMETRY，
 * 另有 GEOM_LEN（本系统维护的几何字节长度）和 GEOM_BACKUP（CLOB 备份）两列。
 * 几何的读写由 GisKgAll200020251209Mapper 里的显式 SQL 负责。
 *
 * @author saber
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("GIS_KG_ALL_2000_20251209")
public class GisKgAll200020251209Entity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键。
     * DDL 里是 INT NOT NULL，没有 IDENTITY 子句，所以用 IdType.INPUT，插入前需自行赋值；
     * 若库里实际是 IDENTITY 或由序列赋值，改成 IdType.AUTO 即可。
     */
    @TableId(value = "OBJECTID", type = IdType.INPUT)
    private Integer objectid;

    /**
     * 几何（SYSGEO.ST_GEOMETRY），内存里以 WKB 的 byte[] 形态存放。
     * <p>
     * select / insert / update 全部设为不参与：避免 MyBatis-Plus 生成的 SQL 把达梦几何类型直接当 byte[] 读写。
     * 读由 mapper 用 ST_AsBinary 转换后填充（别名 geom2wkb），写由 mapper 用 ST_GeomFromText 转换。
     */
    @TableField(value = "GEOM", jdbcType = JdbcType.BLOB,
            select = false, insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private byte[] geom2wkb;

    /**
     * 几何备份（CLOB）。
     * 属于大字段且是备份用途，默认不参与查询，避免分页查询把整个备份列拉回来；
     * 需要时用自定义 SQL 单独取。
     */
    @TableField(value = "GEOM_BACKUP", jdbcType = JdbcType.CLOB, select = false)
    private String geomBackup;

    /**
     * 用地用海名称
     */
    @TableField("YDYMC")
    private String ydymc;

    /**
     * 用地代码
     */
    @TableField("YDDM")
    private String yddm;

    /**
     * 用地用海分类代码
     */
    @TableField("YDYHFLDM")
    private String ydyhfldm;

    /**
     * 用地代码（后核）
     */
    @TableField("YDDMHH")
    private String yddmhh;

    /**
     * 用地名称
     */
    @TableField("YDMC")
    private String ydmc;

    /**
     * 标准单元编号
     */
    @TableField("BZDYBH")
    private String bzdybh;

    /**
     * 项目名称
     */
    @TableField("XMMC")
    private String xmmc;

    /**
     * 台账编号
     */
    @TableField("TZBH")
    private String tzbh;

    /**
     * 地块编号
     */
    @TableField("DKBH")
    private String dkbh;

    /**
     * 地上容积率上限
     */
    @TableField("DSRJLSX")
    private Double dsrjlsx;

    /**
     * 地上容积率下限
     */
    @TableField("DSRJLXX")
    private Double dsrjlxx;

    /**
     * 建筑高度上限
     */
    @TableField("JZGDSX")
    private Double jzgdsx;

    /**
     * 建筑高度下限
     */
    @TableField("JZGDXX")
    private Double jzgdxx;

    /**
     * 绿地率上限
     */
    @TableField("LDLSX")
    private Double ldlsx;

    /**
     * 绿地率下限
     */
    @TableField("LDLXX")
    private Double ldlxx;

    /**
     * 建筑密度上限
     */
    @TableField("JZMDSX")
    private Double jzmdsx;

    /**
     * 建筑密度下限
     */
    @TableField("JZMDXX")
    private Double jzmdxx;

    /**
     * 数据版本
     */
    @TableField("VERSION")
    private Integer version;

    /**
     * 几何字节长度，本系统维护，用于大几何的缓存分流
     */
    @TableField("GEOM_LEN")
    private Integer geomLen;

}
