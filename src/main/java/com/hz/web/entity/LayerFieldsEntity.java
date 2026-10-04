package com.hz.web.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author saber
 * @since 2025-05-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("layerFields")
public class LayerFieldsEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;


    /**
     * 图层id
     */
    @TableField("layer_id")
    private Integer layerId;

    /**
     * 空间表名
     */
    @TableField("layer_name")
    private String layerName;

    /**
     * 字段英文名
     */
    @TableField("field_name_en")
    private String fieldNameEn;

    /**
     * 字段中文名
     */
    @TableField("field_name_zh")
    private String fieldNameZh;

    /**
     * 字段类型
     */
    @TableField("field_type")
    private String fieldType;

    /**
     * 字段长度
     */
    @TableField("field_len")
    private String fieldLen;

    /**
     * 字段是否可为空（0=不可为空  1=可为空）
     */
    @TableField("field_nullable")
    private Integer fieldNullable;

    /**
     * 字段分组类型， 字典
     */
    @TableField("field_group_type")
    private Integer fieldGroupType;

    /**
     * 字段排序
     */
    @TableField("field_order")
    private Integer fieldOrder;

    /**
     * 字段值对于的字典（如果有字典就填）
     */
    @TableField("field_value_dict_name")
    private String fieldValueDictName;

    /**
     * 字段值对应的规则（如果有规则就填）
     */
    @TableField("field_value_rule_name")
    private String fieldValueRuleName;

    // 不是数据库字段
    @TableField(exist = false)
    private Object fieldValue;
}
