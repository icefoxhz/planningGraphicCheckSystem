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
 * @since 2025-06-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("vectorLayer")
public class VectorLayerEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id")
    private Integer id;

    /**
     * cad图层名
     */
    @TableField("cad_layer_tmp")
    private String cadLayer;

    /**
     * 数据库表名
     */
    @TableField("gis_layer")
    private String gisLayer;

    /**
     * 图层类型， 0=无类型  1=点  2=线  3=面
     */
    @TableField("gis_layer_type")
    private Integer gisLayerType;

    /**
     * 图层颜色
     */
    @TableField("color")
    private Integer color;

    /**
     * 线类型
     */
    @TableField("line_type")
    private String lineType;

    /**
     * 线宽
     */
    @TableField("line_width")
    private Integer lineWidth;

    /**
     * 是否填充
     */
    @TableField("is_hatch")
    private Integer isHatch;

    /**
     * 符号化code
     */
    @TableField("code_field")
    private String codeField;

    /**
     * 绘制管点用到
     */
    @TableField("codes")
    private String codes;

    /**
     * 是否添加xdata
     */
    @TableField("is_add_xdata")
    private Integer isAddXdata;

    /**
     * 主类型（管点/管线）
     */
    @TableField("major_type")
    private Integer majorType;

    /**
     * 是否支持附件树  0 = 不支持   1 = 支持。 默认 0
     */
    @TableField("support_attachment_tree")
    private Integer supportAttachmentTree;


}
