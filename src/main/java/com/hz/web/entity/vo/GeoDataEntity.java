package com.hz.web.entity.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.ibatis.type.JdbcType;

import java.io.Serializable;

/**
 * @author saber
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "geo图层", description = "用于存放查询出来的geom的二进制")
public class GeoDataEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "objectid", type = IdType.AUTO)
    private Integer objectid;

    @TableField(value = "geom", jdbcType = JdbcType.BLOB)
    private byte[] geom2wkb;
}
