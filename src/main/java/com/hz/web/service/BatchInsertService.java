package com.hz.web.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hz.constant.MyConstant;
import com.hz.utils.ConvertUtil;
import com.hz.utils.ZLibUtil;
import com.hz.web.config.BuildingFieldMapperConfig;
import com.hz.web.entity.vo.RedLineUpdateVoEntity;
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
    DynamicDxtKgDataService dynamicDataService;

    @Autowired
    RedisQueueService redisQueueService;

    @Autowired
    GeoDataMapper geoDataMapper;

    @Autowired
    GisKgAll200020251209Mapper gisKgAll200020251209Mapper;

    @Autowired
    BuildingFieldMapperConfig buildingFieldMapperConfig;

    @Autowired
    RedLineVoService redLineVoService;

    @Value("${myProject.cache.dataCompress}")
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

    // 附件目录字段名，每条记录都会带上。注意：入库时 map 的 key 会被当成列名，
    // 目标表必须有这一列，否则 insert 会报字段不存在
    final String ATTACHMENT_DIR_FIELD = "ATTACHMENT_DIR";

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
            // 附件目录
            String attachment_dir = jsonObject.getString("attachment_dir");
            if (attachment_dir == null) {
                attachment_dir = "";
            }

            // doType   0 = 控规    1=建筑
            if (doType == 0) {
                // 获取范围表和数据表的表名
                String range_tableName = kg_range_tableName;
                String data_tableName = kg_data_tableName;
                JSONArray range_tableName_data = jsonObject.getJSONArray("range_tableName_data");
                JSONArray data_tableName_data = jsonObject.getJSONArray("data_tableName_data");

                List<Map<String, Object>> range_tableName_data_list = (List<Map<String, Object>>) (List<?>) range_tableName_data.toJavaList(Map.class);
                List<Map<String, Object>> data_tableName_data_list = (List<Map<String, Object>>) (List<?>) data_tableName_data.toJavaList(Map.class);

                // 每条记录都带上附件目录（范围表和数据表都要）
                putAttachmentDir(range_tableName_data_list, attachment_dir);
                putAttachmentDir(data_tableName_data_list, attachment_dir);

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
                JSONArray building_single_data = jsonObject.getJSONArray("data_tableName_data");
                JSONArray building_single_text_data = jsonObject.getJSONArray("bg_tableName_data");
                JSONObject hx_tableName_data = jsonObject.getJSONObject("hx_tableName_data");
                // 把 hx_tableName_data 套到 JSONArray
                // 这样下面的处理逻辑就跟建筑、文本数据完全一致，不用为单条数据开特例
                JSONArray redline_tableName_data = new JSONArray();
                if (hx_tableName_data != null) {
                    redline_tableName_data.add(hx_tableName_data);
                }

                List<Map<String, Object>> building_single_data_list = (List<Map<String, Object>>) (List<?>) building_single_data.toJavaList(Map.class);
                List<Map<String, Object>> building_single_text_data_list = (List<Map<String, Object>>) (List<?>) building_single_text_data.toJavaList(Map.class);
                List<Map<String, Object>> redline_tableName_data_list = (List<Map<String, Object>>) (List<?>) redline_tableName_data.toJavaList(Map.class);

                // 中文key转英文key
                // 映射表见 config/application-building.yml 的 building-mappers 节点
                convertKeysByMapper(building_single_data_list, buildingFieldMapperConfig.getBuildingFieldMapper());
                convertKeysByMapper(building_single_text_data_list, buildingFieldMapperConfig.getTextFieldMapper());
                convertKeysByMapper(redline_tableName_data_list, buildingFieldMapperConfig.getRedlineFieldMapper());

                // 每条记录都带上附件目录
                putAttachmentDir(building_single_data_list, attachment_dir);
                putAttachmentDir(building_single_text_data_list, attachment_dir);
                putAttachmentDir(redline_tableName_data_list, attachment_dir);

                // 建筑入库
                batchInsert(queueKey, building_single_tableName, now, building_single_data_list, doType);
                // 建筑文本入库
                batchInsert(queueKey, building_single_text_tableName, now, building_single_text_data_list, doType);
                // 更新红线
                redline_tableName_data_list.forEach(map -> {
                    // 找到建筑的红线
                    String wkt = map.get(MyConstant.GEOM_FIELD_NAME).toString();
                    Long redlineObjectId = geoDataMapper.getSimilarityRedlineObjectId(wkt, redline_tableName, redline_similarity);
                    if (redlineObjectId != null) {
                        System.out.println("找到红线ObjectId: " + redlineObjectId);

                        // 更新建筑的红线属性
                        // key 用 redline_field_mapper 映射出来的物理列名，注意都是小写
                        // （映射表见 config/application-building.yml，这时候还没走 batchInsert 的统一转大写）
                        RedLineUpdateVoEntity redLineUpdateVoEntity = new RedLineUpdateVoEntity();
                        redLineUpdateVoEntity
                                .setObjectId(redlineObjectId)
                                .setBuilddensi(getStringValue(map, "BUILDDENSI"))
                                .setLdr(getStringValue(map, "LDR"))
                                .setGpr(getStringValue(map, "GPR"))
                                .setB_height(getStringValue(map, "B_HEIGHT"))
                                .setJdctcw(getStringValue(map, "JDCTCW"))
                                .setFjdctcw(getStringValue(map, "FJDCTCW"));

                        // MyBatis-Plus 的 updateById 只拼非 null 字段，全为 null 会生成
                        // "update 表 set where objectid=?" 这种语法错误的 SQL，所以先挡一道
                        if (hasUpdateField(redLineUpdateVoEntity)) {
                            redLineVoService.redlineUpdate(redLineUpdateVoEntity);
                        } else {
                            log.warn("红线 {} 没有拿到任何待更新字段（builddensi/gpr/b_height/ldr/jdctcw/fjdctcw 全为空），跳过更新。queueKey={}",
                                    redlineObjectId, queueKey);
                        }
                    }
                });

            }

        }
    }


    /**
     * 取一行数据里某个字段的字符串值，字段不存在或值为 null 时返回 null（不会 NPE）。
     *
     * @param row       一行数据
     * @param fieldName 物理列名，用 {@code redline_field_mapper} 映射出来的名字，都是小写
     */
    private String getStringValue(Map<String, Object> row, String fieldName) {
        if (!row.containsKey(fieldName)) {
            return "";
        }
        return row.get(fieldName).toString();
    }

    /**
     * 判断更新实体里有没有至少一个待更新字段。
     *
     * <p>MyBatis-Plus 的 {@code updateById} 只把非 null 的字段拼进 SET 子句，
     * 全为 null 时会生成 {@code update 表 set where objectid = ?} 这种语法错误的 SQL，
     * 所以在调用前先挡一道，给出更清楚的日志。</p>
     */
    private boolean hasUpdateField(RedLineUpdateVoEntity entity) {
        return entity.getBuilddensi() != null
                || entity.getLdr() != null
                || entity.getGpr() != null
                || entity.getB_height() != null
                || entity.getJdctcw() != null
                || entity.getFjdctcw() != null;
    }


    /**
     * 给数据里每条记录补上附件目录字段（{@link #ATTACHMENT_DIR_FIELD}）。
     *
     * <p>附件目录是从 redis 消息的 {@code attachment_dir} 里取的，按「每批」一个值，
     * 同一条消息里的所有记录共用同一份。取不到时是空串，不会塞 null。</p>
     *
     * <p><b>注意：</b>{@link DynamicDxtKgDataService#batchInsert} 是拿 map 的 key 当列名动态拼
     * insert 语句的，所以加了字段就必须保证目标表真的有这一列，否则批量插入会整批失败，
     * 然后退化成逐条插入，最终每条都会被丢进失败队列。</p>
     *
     * @param dataList      待处理的数据，直接原地修改
     * @param attachmentDir 附件目录，允许为空串
     */
    private void putAttachmentDir(List<Map<String, Object>> dataList, String attachmentDir) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }
        for (Map<String, Object> row : dataList) {
            if (row != null) {
                row.put(ATTACHMENT_DIR_FIELD, attachmentDir);
            }
        }
    }


    /**
     * 把数据里的中文 key 按映射表换成英文（物理字段名）。
     *
     * <p>映射表里没有的 key 原样保留 —— 前端传过来的 OBJECTID、GEOM 这些本来就是英文，
     * 不需要也不能被映射掉。</p>
     *
     * <p>需要映射成什么，由 {@code config/application-building.yml} 的
     * {@code building-mappers} 节点决定；yml 里的中文 key 必须写成 {@code "[建筑密度]"}
     * 这种带中括号的形式，否则 Spring 绑不进来（详见 {@link BuildingFieldMapperConfig}）。</p>
     *
     * <p>就地替换 List 里的元素，不重新赋值外层变量 —— 这样调用方的局部变量仍然是
     * effectively final，后面还能继续在 lambda 里引用。</p>
     *
     * @param dataList    待转换的数据，直接原地修改
     * @param fieldMapper 中文标注名 -&gt; 物理字段名，取自 {@link BuildingFieldMapperConfig}
     */
    private void convertKeysByMapper(List<Map<String, Object>> dataList, Map<String, String> fieldMapper) {
        if (dataList == null || dataList.isEmpty() || fieldMapper == null || fieldMapper.isEmpty()) {
            return;
        }
        for (int i = 0; i < dataList.size(); i++) {
            Map<String, Object> row = dataList.get(i);
            if (row == null || row.isEmpty()) {
                continue;
            }
            // 用 LinkedHashMap 保持字段顺序，另外这里也不能改一个 key 删一个，
            // 会踩到 ConcurrentModificationException
            Map<String, Object> converted = new LinkedHashMap<>(row.size());
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String fieldName = fieldMapper.get(entry.getKey());
                converted.put(fieldName == null ? entry.getKey() : fieldName, entry.getValue());
            }
            dataList.set(i, converted);
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

            if (mapNew.containsKey(MyConstant.ID_FIELD_NAME)) {
                objectIdList.add(mapNew.get(MyConstant.ID_FIELD_NAME).toString());
            }

            HashMap<String, Object> cloneData = new HashMap<>(mapNew);
            // 去掉objectid, 这个在数据库中要自动生成
            if (mapNew.containsKey(MyConstant.ID_FIELD_NAME)) {
                cloneData.remove(MyConstant.ID_FIELD_NAME);
            }
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
//        ConcurrentHashMap<String, Object> stringObjectConcurrentHashMap = insertProgressMap.get(queueKey);
//
//        String oldMsg = stringObjectConcurrentHashMap.get("msg").toString();
//        String newMsg = oldMsg + (rollbackCause == null ? "," : (" " + rollbackCause + ", "));
//        try {
//            String insertTime = insertInfoMap.get(queueKey).get("tm").toString();
//            String userName = insertInfoMap.get(queueKey).get("userName").toString();
//            String tableName = insertInfoMap.get(queueKey).get("tableName").toString();
//            boolean hadWkt = (boolean) insertInfoMap.get(queueKey).get("hadWkt");
//            if (insertTime == null || userName == null || tableName == null) {
//                return;
//            }
//            stringObjectConcurrentHashMap.put("errCount", Integer.parseInt(stringObjectConcurrentHashMap.get("errCount").toString()) + 1);
//            stringObjectConcurrentHashMap.put("msg", newMsg + "正在回滚......");
//
//            dynamicDataService.rollback(userName, tableName, insertTime, hadWkt);
//
//            stringObjectConcurrentHashMap.put("msg", newMsg + "回滚完成!");
//        } catch (Exception e) {
//            stringObjectConcurrentHashMap.put("msg", newMsg + "回滚失败! 错误: " + e.getMessage());
//        }
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
