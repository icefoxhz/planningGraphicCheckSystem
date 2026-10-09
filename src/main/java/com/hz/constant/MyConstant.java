package com.hz.constant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author saber
 */

@Component
public class MyConstant {
    @Value("${myProject.geoTable.idFieldName}")
    private String idFieldNameProp;

    @Value("${myProject.geoTable.geoFieldName}")
    private String geoFieldNameProp;

    @Value("${myProject.geoTable.dmgeo:dmgeo}")
    private String dmgeoProp;

    @Value("${myProject.dm.blob2binaryLenLimit}")
    private int blob2binaryLenLimit;

//    @Value("${login.retryTimes}")
//    private int retryTimes;
//
//    @Value("${login.lockDuration}")
//    private int lockDuration;

    // 静态字段供外部使用
    public static String ID_FIELD_NAME;
    public static String GEOM_FIELD_NAME;
    /**
     * 空间函数包名前缀，由 myProject.geoTable.dmgeo 配置，取 dmgeo 或 dmgeo2。
     * 达梦 DMGEO 与 DMGEO2 两套包的类型和函数定义互不兼容，必须与库里 GEOM 列的类型属主配套。
     */
    public static String SPATIAL_PKG = "dmgeo2";
    public static int BLOB_TO_BINARY_LEN_LIMIT;
    public static final String WKB_FIELD_NAME = "GEO2WKB";   // 程序内部自己用的
    public static final String GEOM_LEN_FIELD_NAME = "GEOM_LEN";   // 程序内部自己用的
    public static ConcurrentSkipListSet<String> GEOM_TABLES = new ConcurrentSkipListSet<>();  // 用到的空间表名

    // 大的geom缓存起来，就不需要每次转换了
    public static ConcurrentHashMap<String, ConcurrentHashMap<Integer, Object>> LAYER_ID_GEOM_BIG_BINARY_CACHE = new ConcurrentHashMap<>();

    public static String USER_TOKEN_OUTER_KEY_NAME = "userToken_";
    public static int USER_TOKEN_EXPIRE_HOURS = 24;

    // 登录设置
//    public static Integer LOGIN_RETRY_TIMES = 0;
//    public static Integer LOGIN_LOCK_DURATION = 0;
//    public static ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> USER_LOGIN_MAP = new ConcurrentHashMap<>();


    // 在Bean初始化后赋值给静态变量
    @PostConstruct
    public void init() {
        ID_FIELD_NAME = idFieldNameProp;
        GEOM_FIELD_NAME = geoFieldNameProp;
        BLOB_TO_BINARY_LEN_LIMIT = blob2binaryLenLimit;
//        LOGIN_RETRY_TIMES = retryTimes;
//        LOGIN_LOCK_DURATION = lockDuration;

        if (dmgeoProp != null && !dmgeoProp.trim().isEmpty()) {
            SPATIAL_PKG = dmgeoProp.trim();
        }
    }

    /**
     * 几何类型属主，与 SPATIAL_PKG 配套：dmgeo -> SYSGEO，dmgeo2 -> SYSGEO2。
     * 建表时 GEOM 列的类型应写成 {@code getGeomTypeOwner() + ".ST_Geometry"}。
     */
    public static String getGeomTypeOwner() {
        return SPATIAL_PKG != null && SPATIAL_PKG.toUpperCase().endsWith("2") ? "SYSGEO2" : "SYSGEO";
    }
}
