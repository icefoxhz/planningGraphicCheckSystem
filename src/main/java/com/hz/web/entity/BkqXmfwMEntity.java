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
import java.util.Date;

/**
 * 项目范围面 BKQ_XMFW_M
 * <p>
 * 注意几何列名是 SHAPE（不是 yml 里配的 geoFieldName=GEOM），类型为 SYSGEO.ST_GEOMETRY，
 * 因此几何的读写不能走 GeoDataProvider 那套通用逻辑，由 BkqXmfwMMapper 里的显式 SQL 负责。
 *
 * @author saber
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("BKQ_XMFW_M")
public class BkqXmfwMEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键。
     * 该表 OBJECTID 不是自增/标识列（DDL 里只有 INT），所以用 IdType.INPUT，插入前需自行赋值；
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
     * 要素代码
     */
    @TableField("YSDM")
    private String ysdm;

    /**
     * 要素名称
     */
    @TableField("YSMC")
    private String ysmc;

    /**
     * 行政区代码
     */
    @TableField("XZQDM")
    private String xzqdm;

    /**
     * 行政区名称
     */
    @TableField("XZQMC")
    private String xzqmc;

    /**
     * 标准单元编号
     */
    @TableField("BZDYBH")
    private String bzdybh;

    /**
     * 项目编号
     */
    @TableField("XMBH")
    private String xmbh;

    /**
     * 项目流水号
     */
    @TableField("XMLSH")
    private String xmlsh;

    /**
     * 项目名称
     */
    @TableField("XMMC")
    private String xmmc;

    /**
     * 规划类型
     */
    @TableField("GHLX")
    private String ghlx;

    /**
     * 规划范围
     */
    @TableField("GHFW")
    private String ghfw;

    /**
     * 规划面积
     */
    @TableField("GHMJ")
    private Double ghmj;

    /**
     * 规划人口
     */
    @TableField("GHRK")
    private Integer ghrk;

    /**
     * 现状基准年
     */
    @TableField("XZJZN")
    private String xzjzn;

    /**
     * 规划起始年
     */
    @TableField("GHQSN")
    private String ghqsn;

    /**
     * 规划目标年
     */
    @TableField("GHMBN")
    private String ghmbn;

    /**
     * 当前版本
     */
    @TableField("DQBB")
    private String dqbb;

    /**
     * 委托单位
     */
    @TableField("WTDW")
    private String wtdw;

    /**
     * 编制单位
     */
    @TableField("BZDW")
    private String bzdw;

    /**
     * 审批单位
     */
    @TableField("SPDW")
    private String spdw;

    /**
     * 完成时间
     */
    @TableField("WCSJ")
    private Date wcsj;

    /**
     * 批准时间
     */
    @TableField("PZSJ")
    private Date pzsj;

    /**
     * 入库单位
     */
    @TableField("RKDW")
    private String rkdw;

    /**
     * 入库人员
     */
    @TableField("RKRY")
    private String rkry;

    /**
     * 入库时间
     */
    @TableField("RKSJ")
    private Date rksj;

    /**
     * 备注
     */
    @TableField("BZ")
    private String bz;
}
