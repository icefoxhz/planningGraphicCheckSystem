package com.hz.utils;


import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * @author saber
 */

@Slf4j
public class P6SpyLogger implements MessageFormattingStrategy {

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String s4) {
//        String log = !"".equals(sql.trim()) ? this.format.format(new Date()) + " | took " + elapsed + "ms | " + category + " | connection " + connectionId + "\n " + " >>>>>>>>> 真实sql语句 = " + sql + ";" : "";
        //        log.info(logStr);
        return !sql.trim().isEmpty() ? "====== connection " + connectionId  + " => " + sql + ";" : "";
    }
}