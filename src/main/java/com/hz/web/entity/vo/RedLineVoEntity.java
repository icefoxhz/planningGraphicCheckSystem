package com.hz.web.entity.vo;

//分类 =>  op_type     		int
//红线类型 =>  xtype    		string
//出图日期 =>  print_date   	string
//用地性质 =>  b_landuse    	string
//地块号 => dkh			 	string
//经办人 => workman		 	string
//项目名称 => b_projectname  	string
//申报单位 => plot_owner 		string

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
//@TableName("layerTree")
@ApiModel(value = "红线图层", description = "")
public class RedLineVoEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer objectId;

    @JsonProperty("opType")
    private List<Integer> opType;

    @JsonProperty("xType")
    private String xType;

    private String printDate;

    private String printStartDate;

    private String printEndDate;

    @JsonProperty("bLanduse")
    private String bLanduse;

    private String dkh;

    @JsonProperty("workman")
    private String workman;

    @JsonProperty("bProjectname")
    private String bProjectname;

    @JsonProperty("plotOwner")
    private String plotOwner;

    private Float area;
}
