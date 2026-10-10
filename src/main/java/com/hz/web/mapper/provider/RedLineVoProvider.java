package com.hz.web.mapper.provider;

import com.hz.constant.MyConstant;
import com.hz.web.entity.vo.RedLineVoEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.jdbc.SQL;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类 =>  op_type     		int
 * 红线类型 =>  xtype    		string
 * 出图日期 =>  print_date   	string
 * 用地性质 =>  b_landuse    	string
 * 地块号 => dkh			 	string
 * 经办人 => workman		 	string
 * 项目名称 => b_projectname  	string
 * 申报单位 => plot_owner 		string
 *
 * @author saber
 */
public class RedLineVoProvider {
    public String queryRedLine(@Param("redLineVoEntity") RedLineVoEntity redLineVoEntity, @Param("tableName") String tableName) {
        List<Integer> opType = redLineVoEntity.getOpType();
        String printStartDate = redLineVoEntity.getPrintStartDate();
        String printEndDate = redLineVoEntity.getPrintEndDate();
        String bLanduse = redLineVoEntity.getBLanduse();
        String dkh = redLineVoEntity.getDkh();
        String workman = redLineVoEntity.getWorkman();
        String bProjectname = redLineVoEntity.getBProjectname();
        String plotOwner = redLineVoEntity.getPlotOwner();


        SQL sql = new SQL().SELECT(String.format(MyConstant.ID_FIELD_NAME + ",op_type, dkh, xtype," +
                        "TO_CHAR(TO_DATE(print_date, 'YYYY-MM-DD HH24:MI:SS.FF6'), 'YYYY-MM-DD') AS print_date," +
                        "plot_owner, workman, dmgeo2.st_area(%s) as area, b_landuse, b_projectname", MyConstant.GEOM_FIELD_NAME))
                .FROM(tableName);

        if (opType != null) {
            String inClause = opType.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ", "(", ")"));
            sql.WHERE(String.format("op_type in %s", inClause));
        }

        // // 加一天
        if (printStartDate.equals(printEndDate)) {
            LocalDate end = LocalDate.parse(printEndDate).plusDays(1);
            printEndDate = end.toString(); // 格式化成 yyyy-MM-dd
        }
        sql.WHERE(String.format("print_date >=date_format('%s','yyyy-mm-dd') and print_date<=date_format('%s','yyyy-mm-dd')", printStartDate, printEndDate));
        if (!bLanduse.isEmpty()) {
            sql.WHERE("b_landuse like '%" + bLanduse + "%'");
        }

        if (!dkh.isEmpty()) {
            sql.WHERE("dkh like '%" + dkh + "%'");
        }

        if (!workman.isEmpty()) {
            sql.WHERE("workman like '%" + workman + "%'");
        }

        if (!bProjectname.isEmpty()) {
            sql.WHERE("b_projectname like '%" + bProjectname + "%'");
        }

        if (!plotOwner.isEmpty()) {
            sql.WHERE("plot_owner like '%" + plotOwner + "%'");
        }

//        System.out.println(sql.toString());
        return sql.toString();
    }
}
