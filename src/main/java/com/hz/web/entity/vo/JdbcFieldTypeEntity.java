package com.hz.web.entity.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author saber
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "字段信息", description = "根据jdbc获取的字段信息")
public class JdbcFieldTypeEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fieldName;

    private String fieldType;

    private int fieldLen;

    private String nullAble;
}
