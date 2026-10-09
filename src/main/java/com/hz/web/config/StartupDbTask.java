package com.hz.web.config;

import com.hz.web.service.AbsCacheService;
import com.hz.web.service.CacheServiceManager;
import com.hz.web.service.TableMetadataService;
import lombok.extern.slf4j.Slf4j;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import org.gdal.osr.SpatialReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author saber
 */
@Component
@Slf4j
public class StartupDbTask implements ApplicationRunner {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    TableMetadataService tableMetadataService;

    @Autowired
    CacheServiceManager cacheServiceManager;

    @Value("${myProject.cache.strategy}")
    private String strategy;

    @Value("${myProject.cache.is_start_init}")
    private boolean isStartInit;

//    public void initGDAL() {
//        ogr.RegisterAll();
//        // 为了支持中文路径，请添加下面这句代码
//        gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8","YES");
//        // 为了使属性表字段支持中文，请添加下面这句
//        gdal.SetConfigOption("SHAPE_ENCODING","CP936");
//
//        // mdb连接串
//        gdal.SetConfigOption("PGEO_DRIVER_TEMPLATE", "DRIVER=Microsoft Access Driver (*.mdb, *.accdb);DBQ=%s");
//        gdal.SetConfigOption("MDB_DRIVER_TEMPLATE", "DRIVER=Microsoft Access Driver (*.mdb, *.accdb);DBQ=%s");
//    }

//    public static void test_read_mdb(){
//        String mdbFile = "E:\\A0205-A0208.mdb";
//
//        Driver driver = ogr.GetDriverByName("PGeo");
//        System.out.println(driver);
//
//        DataSource ds = driver.Open(mdbFile);
//        if (ds == null){
//            System.out.println("打开数据源失败：" + mdbFile);
//            return;
//        }
//
//        int layerCount = ds.GetLayerCount();
//        System.out.println("数据源：" + mdbFile + "，图层数量：" + layerCount);
//
//        for (int i = 0; i < layerCount; i++){
//            Layer layer = ds.GetLayer(i);
//            if (layer == null){
//                continue;
//            }
//
//            System.out.println();
//            System.out.println("================= 图层[" + i + "] =================");
//            System.out.println("图层名称：" + layer.GetName());
//            System.out.println("几何列名：" + layer.GetGeometryColumn());
//            System.out.println("要素个数：" + layer.GetFeatureCount());
//
//            SpatialReference spatialReference = layer.GetSpatialRef();
//            if (spatialReference != null){
//                System.out.println("空间参考：" + spatialReference.GetName());
//            }
//
//            // 字段定义
//            FeatureDefn featureDefn = layer.GetLayerDefn();
//            int fieldCount = featureDefn.GetFieldCount();
//            StringBuilder fieldNames = new StringBuilder();
//            for (int f = 0; f < fieldCount; f++){
//                if (f > 0){
//                    fieldNames.append(", ");
//                }
//                fieldNames.append(featureDefn.GetFieldDefn(f).GetName());
//            }
//            System.out.println("字段数：" + fieldCount + "，字段：" + fieldNames);
//
//            // 遍历图层内所有要素
//            Feature feature;
//            int featureIndex = 0;
//            while ((feature = layer.GetNextFeature()) != null){
//                StringBuilder sb = new StringBuilder();
//                sb.append("  [").append(featureIndex++).append("] FID=").append(feature.GetFID());
//                for (int f = 0; f < fieldCount; f++){
//                    FieldDefn fieldDefn = featureDefn.GetFieldDefn(f);
//                    sb.append(" ").append(fieldDefn.GetName())
//                            .append("=").append(feature.GetFieldAsString(f));
//                }
//                System.out.println(sb);
//
//                Geometry geometry = feature.GetGeometryRef();
//                if (geometry != null){
//                    System.out.println("      几何类型：" + geometry.GetGeometryName());
//                    System.out.println("      WKT：" + geometry.ExportToWkt());
//                } else {
//                    System.out.println("      几何类型：无");
//                }
//
//                feature.delete();
//            }
//        }
//
//        ds.delete();
//    }

    @Override
    public void run(ApplicationArguments args) {
//        initGDAL();
//        test_read_mdb();

        if ("redis".equalsIgnoreCase(strategy)) {
            try {
                redisTemplate.hasKey("test"); // 触发连接
                System.out.println("✅ Redis 启动连接成功");
            } catch (Exception e) {
                System.err.println("❌ Redis 无法连接：" + e.getMessage());
                throw e;
            }
        }

        if (isStartInit) {
            // 程序启动后，创建 geom_len字段，并添加索引
            tableMetadataService.createGeomLenFieldAndIdx();
            log.info("所有空间表初始化完成");

            log.info("开始建立大图形缓存......");
            AbsCacheService cacheService = cacheServiceManager.getCacheService();
            cacheService.initCache();
        }

        log.info(">>>>>> 启动完成 <<<<<<");
    }
}