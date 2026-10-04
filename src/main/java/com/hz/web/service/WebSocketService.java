package com.hz.web.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hz.utils.ConvertUtil;
import com.hz.utils.GeomCovertUtil;
import com.hz.utils.ZLibUtil;
import com.hz.web.mapper.GeoDataMapper;
import com.hz.web.service.impl.GeoDataServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * @author saber
 */
@Service
@Slf4j
public class WebSocketService {
    @Autowired
    GeoDataServiceImpl geoDataService;

    @Autowired
    GeoDataMapper geoDataMapper;

    @Autowired
    TableMetadataService tableMetadataService;

    @Autowired
    CacheServiceManager cacheServiceManager;

    @Autowired
    DynamicDataService dynamicDataService;


    @Value("${myProject.webSocket.dataCompress}")
    private boolean dataCompress;

    @Value("${myProject.cache.superCacheThreshold}")
    private int superCacheThreshold;

    @Value("${myProject.dm.blob2binaryLenLimit}")
    private int blob2binaryLenLimit;

    // opType的值如下
    final int DRAW = 0;                             // 调图
//    final int INSERT_DATA_FROM_QUEUE = 1;           // 从队列中读取数据入库
//    final int PUT_DATA_TO_QUEUE = 2;                // 把数据写入队列
//    final int GET_INSERT_INFO = 3;                  // 获取入库结果信息

    /**
     * 当前websocket连接集合
     */
    public static final ConcurrentHashMap<String, Integer> WEB_SOCKET_OP_MAP = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, WebSocketSession> WEB_SOCKET_SESSION_MAP = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, ConcurrentLinkedQueue<JSONObject>> WEB_SOCKET_SESSION_CACHE_MAP = new ConcurrentHashMap<>();

    public void handleConnect(WebSocketSession session) {
        log.info("Connected: {}", session.getId());
        WEB_SOCKET_SESSION_MAP.put(session.getId(), session);
    }

    public void handleMessage(WebSocketSession session, String message) throws IOException {
        log.info("Message from {}: {}", session.getId(), message);
        // 这里你可以处理消息、查询数据库、异步发送等
        processMessage(message, session);
    }

    public void handleClose(WebSocketSession session) throws IOException {
        log.info("Closed: {}", session.getId());

        // 关闭连接
        session.close(CloseStatus.SERVER_ERROR);
        // 删除对象
        WEB_SOCKET_SESSION_MAP.remove(getSessionId(session));
        WEB_SOCKET_SESSION_CACHE_MAP.remove(getSessionId(session));
        WEB_SOCKET_OP_MAP.remove(getSessionId(session));
    }

    public void handleError(WebSocketSession session, Throwable exception) throws IOException {
        log.error("connect【{}】error, exception:{}", session.getId(), exception.getMessage());
        // 如果发送异常，则断开连接
        if (session.isOpen()) {
            session.close();
        }
        WEB_SOCKET_SESSION_MAP.remove(getSessionId(session));
        WEB_SOCKET_SESSION_CACHE_MAP.remove(getSessionId(session));
        WEB_SOCKET_OP_MAP.remove(getSessionId(session));
    }

    /**
     * 自定义判断 sessionId
     *
     * @param session 连接对象
     * @return sessionId
     */
    private String getSessionId(WebSocketSession session) {
//        return (String) session.getAttributes().get("username");
        return session.getId();
    }

    /**
     * 发送消息
     *
     * @param sessionId 对象id
     * @param data      数据
     * @throws IOException IO
     */
    public void sendMessage(String sessionId, Object data) throws IOException {
        sendMessage(sessionId, JSON.toJSONString(data));
    }

    /**
     * 发送消息
     *
     * @param sessionId 对象id
     * @param message   消息
     * @throws IOException IO
     */
    public void sendMessage(String sessionId, String message) throws IOException {
        WebSocketSession webSocketSession = WEB_SOCKET_SESSION_MAP.get(sessionId);
        if (webSocketSession == null || !webSocketSession.isOpen()) {
            log.warn("连接对象【{}】已关闭，无法送消息：{}", sessionId, message);
        } else {
            webSocketSession.sendMessage(new TextMessage(message));
//            logger.info("sendMessage：向{}发送消息：{}", sessionId, message);
        }
    }

    /**
     * 获取所有的连接对象ID
     *
     * @return ids
     */
    public List<String> getSessionIds() {
        Enumeration<String> keys = WEB_SOCKET_SESSION_MAP.keys();
        List<String> ks = new ArrayList<>();
        while (keys.hasMoreElements()) {
            ks.add(keys.nextElement());
        }
        return ks;
    }


    @Async
    public void processMessage(String message, WebSocketSession session) throws IOException {
        /*
            {
                "tableName": "REDLINE",
            }
        */
        JSONObject jsonObject = JSONObject.parseObject(message);

        try {
            // 确定操作类型（调图、入库）
            if (jsonObject.containsKey("callType")) {
                int callType = jsonObject.getIntValue("callType");
                WEB_SOCKET_OP_MAP.put(session.getId(), callType);
            }

            // 根据操作类型执行不同操作
            Integer opType = WEB_SOCKET_OP_MAP.get(session.getId());
            if (opType == null) {
                throw new Exception("无法获取操作类型(opType),请先登录");
            }

            switch (opType) {
                case DRAW: {
                    if (jsonObject.containsKey("tableName")) {
                        queryAndProcessData(session, jsonObject);
                    } else if (jsonObject.containsKey("data")) {
                        sendBatchData(session, jsonObject);
                    }
                }
                break;
//                case PUT_DATA_TO_QUEUE: {
//                    if (jsonObject.containsKey("tableName")) {
//                        putDataToQueue(session, jsonObject);
//                    }
//                }
//                break;
//                case INSERT_DATA_FROM_QUEUE: {
//                    if (jsonObject.containsKey("tableName")) {
//                        insertData(session, jsonObject);
//                    }
//                }
//                break;
//                case GET_INSERT_INFO: {
//                    if (jsonObject.containsKey("tableName")) {
//                        getInsertInfo(session, jsonObject);
//                    }
//                }
//                break;
            }

        } catch (Exception e) {
//            sendMessage(session.getId(), "done");
            sendMessage(session.getId(), "done," + e.getMessage().replace(",", "."));
            log.error("message = {}, error = {}", message, e.toString());
        }
    }

//    // 入库流程
//    // 1. 先启动入库线程
//    // 2. 不断提交数据， 判断总数相同就停止
//    // 3. 可以异步获取入库数量
//    private void insertData(WebSocketSession session, JSONObject jsonObject) throws Exception {
//         /*
//            {
//                "username": "admin",
//                "token": "1234567890",
//                "tableName": "KG",
//                "totalCount": 1000,
//                "callType": 1
//             }
//         */
//        String tableName = jsonObject.getString("tableName").toUpperCase();
//        int totalCount = jsonObject.getIntValue("totalCount");
//        dynamicDataService.insertDataFromQueue(tableName, totalCount);
//
//        sendMessage(session.getId(), "done");
//    }

//    private void putDataToQueue(WebSocketSession session, JSONObject jsonObject) throws Exception {
//        /*
//            {
//                "username": "admin",
//                "token": "1234567890",
//                "tableName": "KG",
//                "callType": 2,
//                "data":
//                [
//                    {
//                        "objectid": 1,
//                        "yddm": "B1",
//                        "ydmc": "商业设施",
//                        "geom": "POLYGON((25646.3745117188 71751.2581176758,25586.3544921875 71748.6381225586,25585.9312744141 71758.4598999023,25583.3516845703 71758.3397216797,25582.4141235352 71780.9799194336,25568.8131103516 71780.4166870117,25563.2130737305 71781.8320922852,25554.7811279297 71781.8911132812,25552.4860839844 71832.6217041016,25573.3674926758 71833.4625244141,25618.5042724609 71835.2802734375,25644.7316894531 71836.4926757812,25644.7316894531 71813.2313232422,25646.3745117188 71751.2581176758))"
//                    },
//                    {
//                        "objectid": 2,
//                        "yddm": "RB",
//                        "ydmc": "商住混合",
//                        "geom": "POLYGON((23911.6182861328 72801.4810791016,23952.3671264648 72708.6157226562,23942.9249267578 72705.0120849609,23937.4271240234 72716.2202758789,23906.9097290039 72786.5253295898,23836.7061157227 72758.1738891602,23829.5913085938 72755.1998901367,23813.4072875977 72797.4412841797,23726.045715332 72762.2946777344,23721.5225219727 72760.4268798828,23715.8516845703 72769.9500732422,23819.328918457 72813.1287231445,23820.2965087891 72810.4411010742,23822.1320800781 72804.8782958984,23830.5897216797 72783.8557128906,23835.7365112305 72771.0784912109,23902.0615234375 72797.794128418,23902.0673217773 72797.7965087891,23902.3397216797 72797.9014892578,23911.6182861328 72801.4810791016))"
//                    },
//                    {
//                        "objectid": 3,
//                        "yddm": "RB",
//                        "ydmc": "商住混合",
//                        "geom": "POLYGON((24388.0861206055 73356.6008911133,24377.0405273438 73376.9694824219,24385.8312988281 73381.7366943359,24396.8768920898 73361.3676757812,24388.0861206055 73356.6008911133))"
//                    }
//                ]
//            }
//        */
//        String tableName = jsonObject.getString("tableName").toUpperCase();
//        JSONArray jsonArray = jsonObject.getJSONArray("data");
//
//        List<Map<String, Object>> result = jsonArray.stream()
//                .map(item -> (Map<String, Object>) item)
//                .collect(Collectors.toList());
//        dynamicDataService.putDataToQueue(tableName, result);
//
//        sendMessage(session.getId(), "done");
//    }

//    private void getInsertInfo(WebSocketSession session, JSONObject jsonObject) throws Exception{
//        String tableName = jsonObject.getString("tableName").toUpperCase();
//        Map<String, Object> insertInfo = dynamicDataService.getInsertInfo(tableName);
//        sendMessage(session.getId(), insertInfo);
//    }

    private void queryAndProcessData(WebSocketSession session, JSONObject jsonObject) throws IOException {
        /*
            {
                "username": "admin",
                "token": "1234567890",
                "callType": 0,
                "tableName":"KG",
                "pts": [[0, 0], [1, 0], [1, 1], [0, 0]],
                "sqlCondition": "intime='999999999999'"
            }
        */

        // 注意别忘了  注意别忘了  注意别忘了  op_type 的控制由前端在  sqlCondition 中组织
        // 注意别忘了  注意别忘了  注意别忘了  op_type 的控制由前端在  sqlCondition 中组织
        // 注意别忘了  注意别忘了  注意别忘了  op_type 的控制由前端在  sqlCondition 中组织

        String tableName = jsonObject.getString("tableName").toUpperCase();
        String wkt = null;
        if (jsonObject.containsKey("pts")) {
            JSONArray pts = jsonObject.getJSONArray("pts");
            if (pts != null && !pts.isEmpty()) {
                wkt = GeomCovertUtil.createWkt(pts);
            }
        }

        String sqlCondition = null;
        if (jsonObject.containsKey("sqlCondition")) {
            sqlCondition = jsonObject.getString("sqlCondition");
            if (!sqlCondition.isEmpty()) {
                sqlCondition = "(" + sqlCondition + ")";
            }
        }

        AbsCacheService cacheService = cacheServiceManager.getCacheService();

        long start = System.currentTimeMillis();

        List<Map<String, Object>> dataList;
        if (blob2binaryLenLimit <= superCacheThreshold) {
            // 数据量大的情况下应该下面写法快
            dataList = geoDataService.listLittleGeom(tableName, wkt, sqlCondition);
            List<Map<String, Object>> attributeWithoutGeomList = geoDataService.listAttributeWithoutGeom(tableName, wkt, sqlCondition);
            ConvertUtil.getBytesFromCache(tableName, attributeWithoutGeomList, cacheService);
            dataList.addAll(attributeWithoutGeomList);
        } else {
            // 通用写法
            List<String> fieldsWithoutGeoField = geoDataMapper.getFieldsWithoutGeoField(tableName);
            // 不查 geo字段，从缓存拿
            dataList = geoDataMapper.list(tableName, fieldsWithoutGeoField, wkt, sqlCondition, true);
            // 小的geo就直接查
            List<Map<String, Object>> list2 = geoDataMapper.list(tableName, fieldsWithoutGeoField, wkt, sqlCondition, false);
            ConvertUtil.convertBlobToBytes(tableName, list2, cacheService);
            dataList.addAll(list2);
        }

        long end = System.currentTimeMillis();
        log.info("sql以及blob处理耗时: {} ms", end - start);

        start = System.currentTimeMillis();
        ConcurrentLinkedQueue<JSONObject> jsonQueue = GeomCovertUtil.dataList2JsonQueue(dataList);
        end = System.currentTimeMillis();
        log.info("生成Geometry并生成结果json处理耗时：{} ms", end - start);

        WEB_SOCKET_SESSION_CACHE_MAP.put(session.getId(), jsonQueue);
//        sendMessage(session.getId(), String.format("ok,%d", jsonQueue.size()));
        sendMessage(session.getId(), String.format("ok,%d,%d", jsonQueue.size(), dataCompress ? 1 : 0));
    }

    private void sendBatchData(WebSocketSession session, JSONObject jsonObject) throws IOException {
        /*
            {
                "data": 10
            }
         */
        int batchSize = jsonObject.getIntValue("data");
//                long start = System.currentTimeMillis();
        ConcurrentLinkedQueue<JSONObject> jsonQueue = WEB_SOCKET_SESSION_CACHE_MAP.get(session.getId());
        if (jsonQueue == null || jsonQueue.isEmpty()) {
            sendMessage(session.getId(), "done");
            return;
        }

        JSONArray cache = new JSONArray();
        while (!jsonQueue.isEmpty()) {
            JSONObject item = jsonQueue.poll();
            cache.add(item);
            if (cache.size() >= batchSize) {
//                    String jsonString = new String(cacheString.getBytes(StandardCharsets.UTF_8), StandardCharsets.US_ASCII);
                sendMessage(session.getId(), dataCompress ? ZLibUtil.compressAndBase64(cache.toJSONString()) : cache.toJSONString());
                cache.clear();
                return;
            }
        }
        if (!cache.isEmpty()) {
            sendMessage(session.getId(), dataCompress ? ZLibUtil.compressAndBase64(cache.toJSONString()) : cache.toJSONString());
            cache.clear();
        }
    }
}



