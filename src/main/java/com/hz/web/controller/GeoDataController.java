package com.hz.web.controller;

import com.hz.web.config.DynamicTableConfig;
import com.hz.web.service.BatchInsertService;
import com.swsk.lib.base.entity.ResponseEntity;
import com.swsk.lib.base.utils.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author saber
 */
@RestController
@RequestMapping("/planning")
public class GeoDataController {
    @Value("${spring.redis.database}")
    private int database;

    @Value("${spring.redis.netHost}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Value("${spring.redis.password}")
    private String password;

    @Autowired
    BatchInsertService batchInsertService;

    @Autowired
    DynamicTableConfig dynamicTableConfig;


    @PostMapping(value = "data/geo/jzKgBatchInsert")
    @ResponseBody
    public ResponseEntity jzKgBatchInsert(@RequestBody Map<String, String> dataMap) {
        try {
            // 0 = 控规    1=建筑
            int doType = dataMap.containsKey("type") ? Integer.parseInt(dataMap.get("type")) : 0;
            String redisKey = dataMap.get("redisKey");
            batchInsertService.start(redisKey, doType);

            Map<String, Object> result = new HashMap<>();
            result.put("queueKey", redisKey);
            result.put("database", database);
            result.put("host", host);
            result.put("port", port);
            result.put("password", Base64.getEncoder().encodeToString(password.getBytes()));

            return ResultUtil.success(result, "success");

        } catch (Exception ex) {
//            System.out.println(ex.toString());
            return ResultUtil.error(null, ex.toString());
        }
    }

    @PostMapping(value = "data/geo/jzKgBatchInsertProgress")
    @ResponseBody
    public ResponseEntity jzKgBatchInsertProgress(@RequestBody Map<String, Object> dataMap) {
        try {
            String queueKey = dataMap.get("queueKey").toString();
//            System.out.println("queueKey = " + queueKey);
            ConcurrentHashMap<String, Object> progress = batchInsertService.getProgress(queueKey);
            return ResultUtil.success(progress, "success");
        } catch (Exception ex) {
//            System.out.println(ex.toString());
            return ResultUtil.error(null, ex.toString());
        }
    }

    @PostMapping(value = "data/geo/jzKgBatchInsertError")
    @ResponseBody
    public ResponseEntity jzKgBatchInsertError(@RequestBody Map<String, Object> dataMap) {
        try {
            Integer start = Integer.parseInt(dataMap.get("start").toString());
            Integer end = Integer.parseInt(dataMap.get("end").toString());
            String queueKey = dataMap.get("queueKey").toString();
            List<String> errorData = batchInsertService.getErrorData(queueKey, start, end);
            return ResultUtil.success(errorData, "success");
        } catch (Exception ex) {
//            System.out.println(ex.toString());
            return ResultUtil.error(null, ex.toString());
        }
    }

    /**
     * 获取 application-kgDxt.yml 里配置的表清单，结构跟 yml 保持一致。
     * <p>kg 是「年份 -> {table, ftpDir}」，dxt 是「年份 -> 比例尺 -> ftp 文件路径」；dxt 的比例尺 key
     * 已经由 {@code DynamicTableConfig.scaleLabel()} 补成 {@code "1:500"} 的展示形式
     * （yml 里为了能被正常绑定只能写纯数字，写 {@code "1:500"} 会被 Spring Boot 静默抹掉冒号）。</p>
     * <p>返回示例：</p>
     * <pre>
     * {
     *   "code": 200,
     *   "data": {
     *     "kg": {
     *       "2024": {"table": "kg2024", "ftpDir": "kg/2024"},
     *       "2025": {"table": "kg2025", "ftpDir": "kg/2025"}
     *     },
     *     "dxt": {
     *       "2024": {"1:500": "dxt/2024/500", "1:1000": "dxt/2024/1000"},
     *       "2025": {"1:500": "dxt/2025/500", "1:1000": "dxt/2025/1000"}
     *     }
     *   },
     *   "msg": "success"
     * }
     * </pre>
     */
    @PostMapping(value = "config/dynamicTables")
    @ResponseBody
    public ResponseEntity dynamicTables() {
        try {
            return ResultUtil.success(dynamicTableConfig.toViewMap(), "success");
        } catch (Exception ex) {
            return ResultUtil.error(null, ex.toString());
        }
    }
}
