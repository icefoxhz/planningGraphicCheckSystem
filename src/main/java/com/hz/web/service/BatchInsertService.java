package com.hz.web.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hz.constant.MyConstant;
import com.hz.utils.ConvertUtil;
import com.hz.utils.ZLibUtil;
import com.hz.web.mapper.GeoDataMapper;
import com.hz.web.mapper.GisKgAll200020251209Mapper;
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
 type = 0

 {
     "range_tableName_data":  [{
         "YSDM": "B1",
         "YSMC": "商业设施",
         "GEOM": "POLYGON((25646.3745117188 71751.2581176758,25586.3544921875 71748.6381225586,25585.9312744141 71758.4598999023,25583.3516845703 71758.3397216797,25582.4141235352 71780.9799194336,25568.8131103516 71780.4166870117,25563.2130737305 71781.8320922852,25554.7811279297 71781.8911132812,25552.4860839844 71832.6217041016,25573.3674926758 71833.4625244141,25618.5042724609 71835.2802734375,25644.7316894531 71836.4926757812,25644.7316894531 71813.2313232422,25646.3745117188 71751.2581176758))"
         }],
     "data_tableName_data":[
     {...}
     ]
 }


 type = 1

 {
    "building_single_data":[{...}],
    "building_single_text_data":[{...}]
 }


 */

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

    @Autowired
    GeoDataMapper geoDataMapper;

    @Autowired
    GisKgAll200020251209Mapper gisKgAll200020251209Mapper;

    @Value("${myProject.webSocket.dataCompress}")
    private boolean dataCompress;

    @Value("${myProject.geoTable.srid}")
    private int srid;

    @Value("${myProject.kg_range_tableName}")
    private String kg_range_tableName;

    @Value("${myProject.kg_data_tableName}")
    private String kg_data_tableName;

    @Value("${myProject.building_single_tableName}")
    private String building_single_tableName;

    @Value("${myProject.building_single_text_tableName}")
    private String building_single_text_tableName;

    @Value("${myProject.redline_tableName}")
    private String redline_tableName;

    @Value("${myProject.redline_similarity}")
    private double redline_similarity;

    @Value("${myProject.kg_data_version}")
    private int kg_data_version;

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
    public void start(String queueKey, int doType) {
        // 生成进度
        ConcurrentHashMap<String, Object> progressMap = new ConcurrentHashMap<>();
        progressMap.put("running", true);
        progressMap.put("count", 0);
        progressMap.put("errCount", 0);
        progressMap.put("msg", "");
        insertProgressMap.putIfAbsent(queueKey, progressMap);

        try {
            // 开始
            long startTime = System.currentTimeMillis();
            String now = LocalDateTime.now().format(formatter);
            // 从redis不停读取数据入库，收到 stop终止
            loopInsertDB(queueKey, startTime, now, doType);

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
            System.out.println(">>>>>> 入库完成");
        }
    }

    private void loopInsertDB(String queueKey, long startTime, String now, int doType) {
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

            JSONObject jsonObject = JSONObject.parseObject(msg);

            // doType   0 = 控规    1=建筑
            if (doType == 0) {
                // 获取范围表和数据表的表名
                String range_tableName = kg_range_tableName;
                String data_tableName = kg_data_tableName;
                JSONArray range_tableName_data = jsonObject.getJSONArray("range_tableName_data");
                JSONArray data_tableName_data = jsonObject.getJSONArray("data_tableName_data");

                List<Map<String, Object>> range_tableName_data_list = (List<Map<String, Object>>) (List<?>) range_tableName_data.toJavaList(Map.class);
                List<Map<String, Object>> data_tableName_data_list = (List<Map<String, Object>>) (List<?>) data_tableName_data.toJavaList(Map.class);

                // 范围入库 和 数据更新
                List<Long> fwIdList = new ArrayList<>();
                for (int i = 0; i < range_tableName_data_list.size(); i++) {
                    Map<String, Object> fwData = range_tableName_data_list.get(i);
                    Long id = dynamicDataService.insertAndReturnId(range_tableName, fwData);
                    fwIdList.add(id);
                    // 更新范围数据
                    String wkt = fwData.get(MyConstant.GEOM_FIELD_NAME).toString();
                    gisKgAll200020251209Mapper.updateRangeData(wkt, srid, MyConstant.SPATIAL_PKG);
                }

                // 数据入库
                batchInsert(queueKey, data_tableName, now, data_tableName_data_list, doType);

            }else if (doType == 1) {
                JSONArray building_single_data = jsonObject.getJSONArray("building_single_data");
                JSONArray building_single_text_data = jsonObject.getJSONArray("building_single_text_data");

                List<Map<String, Object>> building_single_data_list = (List<Map<String, Object>>) (List<?>) building_single_data.toJavaList(Map.class);
                List<Map<String, Object>> building_single_text_data_list = (List<Map<String, Object>>) (List<?>) building_single_text_data.toJavaList(Map.class);

                // 建筑入库
                batchInsert(queueKey, building_single_tableName, now, building_single_data_list, doType);
                // 建筑文本入库
                batchInsert(queueKey, building_single_text_tableName, now, building_single_text_data_list, doType);
                // 更新红线
                building_single_data_list.forEach(map -> {
                    // 找到建筑的红线
                    String wkt = map.get(MyConstant.GEOM_FIELD_NAME).toString();
                    Long redlineObjectId = geoDataMapper.getSimilarityRedlineObjectId(wkt, redline_tableName, redline_similarity);
                    if (redlineObjectId != null) {
                        System.out.println("找到红线" + redlineObjectId);
                        // 更新建筑的红线属性

                    }
                });

            }

        }
    }


    @Async
    protected void batchInsert(String queueKey, String tableName, String tm, List<Map<String, Object>> dataList, Integer doType) {
        ConcurrentHashMap<String, Object> progressMap = insertProgressMap.get(queueKey);

        // 涉及到入库时会删除OBJECTID，所以使用克隆。 因为一旦出错要能知道错误数据的OBJECTID
        // 克隆
        List<Map<String, Object>> cloneList = new ArrayList<>();
        List<String> objectIdList = new ArrayList<>();
        for (Map<String, Object> stringObjectMap : dataList) {
            // map的key全部转大写
            Map<String, Object> mapNew = ConvertUtil.convertKeysToUpper(stringObjectMap);

            // 控规数据，VERSION 设为999999999
            if (doType==0) {
                mapNew.put("VERSION", kg_data_version);
            }

            objectIdList.add(mapNew.get(MyConstant.ID_FIELD_NAME).toString());

            HashMap<String, Object> cloneData = new HashMap<>(mapNew);
            // 去掉objectid, 这个在数据库中要自动生成
            cloneData.remove(MyConstant.ID_FIELD_NAME);
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

    String getErrQueueKey(String queueKey) {
        return "err_" + queueKey;
    }
}
