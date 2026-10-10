package com.hz.web.entity.vo;

//分类 =>  op_type     		int
//红线类型 =>  xtype    		string
//出图日期 =>  print_date   	string
//用地性质 =>  b_landuse    	string
//地块号 => dkh			 	string
//经办人 => workman		 	string
//项目名称 => b_projectname  	string
//申报单位 => plot_owner 		string

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 建筑更新红线图层
 *
 * <p><b>这个类必须有 {@code @TableName} 和 {@code @TableId}，否则 MyBatis-Plus 生成的 SQL 是错的：</b></p>
 * <ul>
 *   <li>没有 {@code @TableName}：表名会按类名推导成 {@code red_line_update_vo_entity}，
 *       而不是真实表 {@code gis_红线_正式红线}，直接报「表或视图不存在」。</li>
 *   <li>没有 {@code @TableId}：{@code updateById} 生成的 UPDATE 没有可用的主键列，
 *       WHERE 条件为空，SQL 无法执行。</li>
 * </ul>
 *
 * <p>其余字段名与物理列名完全一致（都是小写、带下划线），MyBatis-Plus 默认的
 * camelToUnderline 转换对这些名字是恒等的，所以不需要额外写 {@code @TableField}。</p>
 *
 * <p>注意 {@code @JsonProperty} 是 Jackson 用的，在 MyBatis-Plus 里<b>不生效</b>，
 * 不能靠它来做字段到列的映射。</p>
 *
 * @author saber
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("gis_红线_正式红线")
@ApiModel(value = "建筑更新红线图层", description = "")
public class RedLineUpdateVoEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键。
     * DDL 里是 {@code objectid BIGINT IDENTITY(1,1) NOT NULL}，但本类的用法是
     * 「先由几何匹配拿到 objectid，再按它更新」，所以用 IdType.INPUT，值由调用方赋值。
     */
    @TableId(value = "objectid", type = IdType.INPUT)
    @JsonProperty("objectid")
    private Long objectId;

    // 建筑密度
    @JsonProperty("builddensi")
    private String builddensi;

    // 建筑高度
    @JsonProperty("b_height")
    private String b_height;

    // 容积率
    @JsonProperty("gpr")
    private String gpr;

    // 绿地率
    @JsonProperty("ldr")
    private String ldr;

    // 机动车位
    @JsonProperty("jdctcw")
    private String jdctcw;

    // 非机动车位
    @JsonProperty("fjdctcw")
    private String fjdctcw;

}
