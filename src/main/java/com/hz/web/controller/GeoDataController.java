package com.hz.web.controller;


//import com.hz.web.service.RedisQueueService;

import com.hz.constant.MyConstant;
import com.hz.utils.ConvertUtil;
import com.hz.web.service.BatchInsertService;
import com.hz.web.service.DynamicDataService;
import com.hz.web.service.impl.GeoDataServiceImpl;
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

    //    @Autowired
//    GeoDataServiceImpl geoDataService;
    @Autowired
    DynamicDataService dynamicDataService;

    @Autowired
    BatchInsertService batchInsertService;


    @PostMapping(value = "data/geo/insert")
    @ResponseBody
    public ResponseEntity insert(@RequestBody Map<String, Object> dataMap) {
        try {
            String tableName = dataMap.get("tableName").toString();
            Object data = dataMap.get("data");

            Long id = dynamicDataService.insertAndReturnId(tableName, ConvertUtil.objectToMap(data));

            return ResultUtil.success(id, "success");
        } catch (Exception ex) {
//            System.out.println(ex.toString());
            return ResultUtil.error(null, ex.toString());
        }
    }

    @PostMapping(value = "data/geo/batchInsert")
    @ResponseBody
    public ResponseEntity batchInsert(@RequestBody Map<String, Object> dataMap) {
        try {
            String userName = dataMap.get("userName").toString();
            String tableName = dataMap.get("tableName").toString().toUpperCase();
            String wkt = dataMap.containsKey("wkt") ? dataMap.get("wkt").toString() : null;

            // 获取时间戳
            String redisKey = String.valueOf(System.currentTimeMillis());
            batchInsertService.start(redisKey, userName, tableName, wkt);
//            batchInsertService.pushTestData(redisKey);
//            batchInsertService.pushTestErrorData(redisKey);

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

    @PostMapping(value = "data/geo/batchInsertRollback")
    @ResponseBody
    public ResponseEntity batchInsertRollback(@RequestBody Map<String, Object> dataMap) {
        try {
            String queueKey = dataMap.get("queueKey").toString();

            batchInsertService.rollback(queueKey, null);

            return ResultUtil.success(null, "success");
        } catch (Exception ex) {
//            System.out.println(ex.toString());
            return ResultUtil.error(null, ex.toString());
        }
    }

    @PostMapping(value = "data/geo/batchInsertError")
    @ResponseBody
    public ResponseEntity batchInsertError(@RequestBody Map<String, Object> dataMap) {
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

    @PostMapping(value = "data/geo/batchInsertProgress")
    @ResponseBody
    public ResponseEntity batchInsertProgress(@RequestBody Map<String, Object> dataMap) {
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

//    @PostMapping(value = "data/geo/batchInsertErrorExport")
//    @ResponseBody
//    public ResponseEntity batchInsertErrorExport(@RequestBody Map<String, Object> dataMap) {
//        try {
//            String queueKey = dataMap.get("queueKey").toString();
//            batchInsertService.exportErrorData(queueKey);
//            return ResultUtil.success(null, "success");
//        } catch (Exception ex) {
////            System.out.println(ex.toString());
//            return ResultUtil.error(null, ex.toString());
//        }
//    }

    @PostMapping(value = "data/geo/remove")
    @ResponseBody
    public ResponseEntity remove(@RequestBody Map<String, Object> dataMap) {
        try {
            String objectId = dataMap.get("objectId").toString();
            String userName = dataMap.get("userName").toString();
            String tableName = dataMap.get("tableName").toString();
            String reason = dataMap.get("reason").toString();
            Integer op = Integer.parseInt(dataMap.get("op").toString());

            dynamicDataService.delete(userName, tableName, reason, op, objectId);

            return ResultUtil.success(null, "success");
        } catch (Exception ex) {
//            System.out.println(ex.toString());
            return ResultUtil.error(null, ex.toString());
        }
    }

    @PostMapping(value = "data/geo/isOverlap")
    @ResponseBody
    public ResponseEntity isOverlap(@RequestBody Map<String, Object> dataMap) {
        try {
            String tableName = dataMap.get("tableName").toString();
            String wkt = dataMap.get("wkt").toString();

            boolean ret = dynamicDataService.isOverlap(wkt, tableName);

            return ResultUtil.success(ret, "success");
        } catch (Exception ex) {
//            System.out.println(ex.toString());
            return ResultUtil.error(null, ex.toString());
        }
    }

    @PostMapping(value = "data/geo/isOverlapEx")
    @ResponseBody
    public ResponseEntity isOverlapEx(@RequestBody Map<String, Object> dataMap) {
        try {
            String tableName = dataMap.get("tableName").toString();
            Map<String, String> wktDict = (Map<String, String>)dataMap.get("wkt");

            ArrayList<String> ret = dynamicDataService.isOverlapEx(wktDict, tableName);

            return ResultUtil.success(ret, "success");
        } catch (Exception ex) {
//            System.out.println(ex.toString());
            return ResultUtil.error(null, ex.toString());
        }
    }
}
