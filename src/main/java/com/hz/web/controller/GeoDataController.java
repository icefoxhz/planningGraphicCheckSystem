package com.hz.web.controller;

import com.hz.web.config.BuildingFieldMapperConfig;
import com.hz.web.service.BatchInsertService;
import com.hz.web.service.DynamicDataService;
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

    @Value("${dxt.scale}")
    private Integer scale;

    @Value("${dxt.ftpDir}")
    private String ftpDir;

    @Value("${myProject.ftpInfo.attachment.dir}")
    private String ftpInfoAttachmentDir;

    @Autowired
    BatchInsertService batchInsertService;

    @Autowired
    BuildingFieldMapperConfig buildingFieldMapperConfig;


    /**
     * 控规/建筑 批量入库。dataMap: {type: 0|1, redisKey: "..."}，
     * 0 = 控规，1 = 建筑。返回 queueKey 与 redis 连接信息（密码 Base64），
     * 数据由前端自己 rightPush 进 redis 队列，后端异步消费。
     */
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
            result.put("ftpAttachmentDir", ftpInfoAttachmentDir);

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

    @PostMapping(value = "config/dxtConfig")
    @ResponseBody
    public ResponseEntity dxtConfig() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("scale", scale);
            result.put("ftpDir", ftpDir);

            return ResultUtil.success(result, "success");
        } catch (Exception ex) {
            return ResultUtil.error(null, ex.toString());
        }
    }

    @PostMapping(value = "config/buildingFieldMapperConfig")
    @ResponseBody
    public ResponseEntity buildingFieldMapperConfig() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("redlineFieldMapper", buildingFieldMapperConfig.getRedlineFieldMapper());
            result.put("buildingFieldMapper", buildingFieldMapperConfig.getBuildingFieldMapper());
            result.put("textFieldMapper", buildingFieldMapperConfig.getTextFieldMapper());

            return ResultUtil.success(result, "success");
        } catch (Exception ex) {
            return ResultUtil.error(null, ex.toString());
        }
    }
}
