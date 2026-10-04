package com.hz.web.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hz.constant.MyConstant;
import com.hz.utils.ConvertUtil;
import com.hz.utils.ZLibUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author saber
 */
@Service
@Slf4j
public class BatchInsertService {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 记录进度
    ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> insertProgressMap = new ConcurrentHashMap<>();
    // 控制入库的回滚
    ConcurrentHashMap<String, Map<String, Object>> insertInfoMap = new ConcurrentHashMap<>();

    @Autowired
    DynamicDataService dynamicDataService;

    @Autowired
    RedisQueueService redisQueueService;

    @Value("${myProject.webSocket.dataCompress}")
    private boolean dataCompress;

//    @Value("${myProject.ftpInfo.batchInsertErrDir}")
//    private String batchInsertErrDir;
//
//    @Value("${myProject.ftpInfo.localRootDir}")
//    private String localRootDir;

    // 7天过期
    final int TIME_OUT = 7;

    final String STOP_FLAG = "stop";

    // 5分钟没数据就退出
    final int AUTO_OVER_TIME = 1000 * 60 * 5;

    @Async
    public void start(String queueKey, String userName, String tableName, String wkt) {
//        queueKey = "testkey";

        // 统一控制操作时间
        String now = LocalDateTime.now().format(formatter);
        Map<String, Object> infoMap = new HashMap<>();
        infoMap.put("tm", now);
        infoMap.put("userName", userName);
        infoMap.put("tableName", tableName);
        // 增量还是范围更新
        infoMap.put("hadWkt", wkt != null);
        insertInfoMap.put(queueKey, infoMap);

        // 生成进度
        ConcurrentHashMap<String, Object> progressMap = new ConcurrentHashMap<>();
        progressMap.put("running", true);
        progressMap.put("count", 0);
        progressMap.put("errCount", 0);
        progressMap.put("msg", "");
        insertProgressMap.putIfAbsent(queueKey, progressMap);

        try {
            // 根据wkt更新实体状态
            if (wkt != null) {
                dynamicDataService.updateEntityHistory(userName, tableName, wkt, now);
            }

            log.info("{} userName: {} ,tableName: {} {}",
                    wkt != null ? "开始范围更新！" : "开始增量入库！",
                    userName,
                    tableName,
                    wkt != null ? ",wkt: " + wkt : "");

            // 开始
            long startTime = System.currentTimeMillis();

            // 从redis不停读取数据入库，收到 stop终止
            loopInsertDB(queueKey, userName, tableName, startTime, now);

            if (Integer.parseInt(progressMap.get("errCount").toString()) > 0) {
                rollback(queueKey, null);
            }

            progressMap.put("running", false);
        } catch (Exception e) {
            rollback(queueKey, e.getMessage());
            progressMap.put("running", false);
        } finally {
            // 设置key的过期时长
            redisQueueService.expireQueue(queueKey, TIME_OUT, TimeUnit.DAYS);
            redisQueueService.expireQueue(getErrQueueKey(queueKey), TIME_OUT, TimeUnit.DAYS);
        }
    }

    private void loopInsertDB(String queueKey, String userName, String tableName, long startTime, String now) {
        while (true) {
            String msg = redisQueueService.popMessage(queueKey);
            if (msg == null) {
//                System.out.println("没有数据");
                long endTime = System.currentTimeMillis();
                long costTime = endTime - startTime;
                if (costTime > AUTO_OVER_TIME) {
//                        redisQueueService.deleteQueue(queueKey);
                    break;
                }

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }

                continue;
            }

            if (STOP_FLAG.equalsIgnoreCase(msg)) {
//                    redisQueueService.deleteQueue(queueKey);
//                System.out.println("收到结束标志 stop");
//                log.info("收到结束标志 stop");
                break;
            }
            // 解压
            if (dataCompress) {
                byte[] decode = Base64.getDecoder().decode(msg);
                msg = new String(ZLibUtil.decompress(decode));
            }

//            System.out.println(msg);
            JSONObject jsonObject = JSONObject.parseObject(msg);
            JSONArray dataList = jsonObject.getJSONArray("dataList");
//            System.out.println(tableName + " => " + dataList);

            // 批量插入
            List<Map<String, Object>> list = (List<Map<String, Object>>) (List<?>) dataList.toJavaList(Map.class);
//            log.info("插入数据: " + list.toString());
            batchInsert(queueKey, userName, tableName, now, list);
        }
    }


    @Async
    protected void batchInsert(String queueKey, String userName, String tableName, String tm, List<Map<String, Object>> dataList) {
        ConcurrentHashMap<String, Object> progressMap = insertProgressMap.get(queueKey);

        // 涉及到入库时会删除OBJECTID，所以使用克隆。 因为一旦出错要能知道错误数据的OBJECTID
        // 克隆
        List<Map<String, Object>> cloneList = new ArrayList<>();
        List<String> objectIdList = new ArrayList<>();
        for (Map<String, Object> stringObjectMap : dataList) {
            // map的key全部转大写
            Map<String, Object> mapNew = ConvertUtil.convertKeysToUpper(stringObjectMap);

            objectIdList.add(mapNew.get(MyConstant.ID_FIELD_NAME).toString());

            HashMap<String, Object> cloneData = new HashMap<>(mapNew);
            // 去掉objectid, 这个在数据库中要自动生成
            cloneData.remove(MyConstant.ID_FIELD_NAME);
            cloneData.put("OP_TYPE", 1);
            cloneData.put("OP_TIME", tm);
            cloneData.put("OP_USER", userName);
            cloneData.put(MyConstant.GEOM_LEN_FIELD_NAME, cloneData.get(MyConstant.GEOM_FIELD_NAME).toString().length());
            cloneList.add(cloneData);
        }

        try {
//            log.info(cloneList.toString());
            dynamicDataService.batchInsert(tableName, cloneList);
            // 更新进度
            progressMap.put("count", (int) progressMap.get("count") + cloneList.size());
        } catch (Exception e) {
            // 批量失败，逐条插入
            for (int i = 0; i < cloneList.size(); i++) {
                try {
                    dynamicDataService.insertAndReturnId(tableName, cloneList.get(i));
                } catch (Exception ex) {
                    log.error(ex.getMessage());

                    // 逐条入库失败, 把数据放入失败redis key中
                    JSONObject errData = new JSONObject();
                    errData.put("queueKey", queueKey);
                    errData.put("tableName", tableName);
                    errData.put("dataId", objectIdList.get(i));
                    errData.put("errorTime", tm);
                    errData.put("errorMsg", ex.getMessage());
                    redisQueueService.pushMessage(getErrQueueKey(queueKey), errData.toJSONString());

                    // 更新进度
                    progressMap.put("errCount", (int) progressMap.get("errCount") + 1);
                    progressMap.put("msg", ex.getMessage());
                }
            }
        }
    }

    public void rollback(String queueKey, String rollbackCause) {
        ConcurrentHashMap<String, Object> stringObjectConcurrentHashMap = insertProgressMap.get(queueKey);

        String oldMsg = stringObjectConcurrentHashMap.get("msg").toString();
        String newMsg = oldMsg + (rollbackCause == null ? "," : (" " + rollbackCause + ", "));
        try {
            String insertTime = insertInfoMap.get(queueKey).get("tm").toString();
            String userName = insertInfoMap.get(queueKey).get("userName").toString();
            String tableName = insertInfoMap.get(queueKey).get("tableName").toString();
            boolean hadWkt = (boolean) insertInfoMap.get(queueKey).get("hadWkt");
            if (insertTime == null || userName == null || tableName == null) {
                return;
            }
            stringObjectConcurrentHashMap.put("errCount", Integer.parseInt(stringObjectConcurrentHashMap.get("errCount").toString()) + 1);
            stringObjectConcurrentHashMap.put("msg", newMsg + "正在回滚......");

            dynamicDataService.rollback(userName, tableName, insertTime, hadWkt);

            stringObjectConcurrentHashMap.put("msg", newMsg + "回滚完成!");
        } catch (Exception e) {
            stringObjectConcurrentHashMap.put("msg", newMsg + "回滚失败! 错误: " + e.getMessage());
        }
    }

    public ConcurrentHashMap<String, Object> getProgress(String queueKey) {
        return insertProgressMap.get(queueKey);
    }

    public List<String> getErrorData(String queueKey, int start, int end) {
        return redisQueueService.getMessage(queueKey, start, end);
    }

//    public void exportErrorData(String queueKey) {
//
//        List<String> errorData = getErrorData(getErrQueueKey(queueKey), 0, -1);
//        if (errorData.isEmpty()) {
//            return;
//        }
//
//        List<ExportBatchInsertErrData> errList = new ArrayList<>();
//        String tableName = null;
//        for (String msg : errorData) {
//            JSONObject jsonObject = JSONObject.parseObject(msg);
//            String dataId = jsonObject.getString("dataId");
//            String errorMsg = jsonObject.getString("errorMsg");
//            errList.add(new ExportBatchInsertErrData(dataId, errorMsg));
//
//            if (tableName == null) {
//                tableName = jsonObject.getString("tableName");
//            }
//        }
//
//        // 写出 Excel
//        String xlsxPath = localRootDir + "/" + batchInsertErrDir + "/" + queueKey + ".xlsx";
//        EasyExcel.write(xlsxPath, ExportBatchInsertErrData.class).sheet(tableName).doWrite(errList);
//        System.out.println("错误导出成功: " + xlsxPath);
//
//        // 清除错误
//        redisQueueService.deleteQueue(getErrQueueKey(queueKey));
//    }

    String getErrQueueKey(String queueKey) {
        return "err_" + queueKey;
    }

    @Async
    public void pushTestData(String queueKey) {
        int i = 0;
        while (i < 100) {
            JSONObject jsonObject = new JSONObject();
            JSONArray dataList = new JSONArray();
            for (int j = 0; j < 3; j++) {
                dataList.add("test" + i + "_" + j);
            }

            jsonObject.put("tableName", "REDLINE");
            jsonObject.put("dataList", dataList);
            String data = jsonObject.toJSONString();
            // 压缩
            if (dataCompress) {
                data = new String(ZLibUtil.compressAndBase64(data));
            }
            redisQueueService.pushMessage(queueKey, data);
            i++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Async
    public void pushTestErrorData(String queueKey) {
        int i = 0;
        while (i < 20) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("queueKey", queueKey);
            jsonObject.put("tableName", "testTable");
            jsonObject.put("dataId", i);
            jsonObject.put("errorMsg", "某种错误" + i);

            redisQueueService.pushMessage(getErrQueueKey(queueKey), jsonObject.toJSONString());
            i++;
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
        }
    }
}
