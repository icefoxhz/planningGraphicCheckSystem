package com.hz;

import com.hz.utils.ZLibUtil;
import com.hz.web.service.DynamicDataService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.gdal.gdal.gdal;
import org.gdal.ogr.*;
import org.gdal.osr.SpatialReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@SpringBootTest
class ProjectTests {

    @Autowired
    DynamicDataService dynamicDataService;

    @Test
    void contextLoads() {
    }

//    @Test
//    void createTableTest() {
//        Map<String, Object> data = new HashMap<>();
//        data.put("objectid", 1);
//        data.put("name", "张飒");
//        data.put("geom", "POINT(120.1233 36.667788)");
//
//        dynamicDataService.createTable("test3", data);
//    }
//
//    @Test
//    void batchInsertTest() throws Exception {
//        List<Map<String, Object>> data = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            Map<String, Object> m = new HashMap<>();
//            m.put("OBJECTID", i);
//            m.put("name", "n" + i);
//            m.put("geom", String.format("POINT(%f %f)", 120.1 + i, 30.2 + i));
//            data.add(m);
//        }
//        dynamicDataService.batchInsert("test3", data, null);
//    }

    public static CellRangeAddress getMergedRegion(Sheet sheet, int rowNum, int colNum) {
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.isInRange(rowNum, colNum)) {
                return region;
            }
        }
        return null;
    }

    public static void unmergeIfMerged(Sheet sheet, int rowIndex, int colIndex) {
        // 获取所有合并区域（新版POI API）
        List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();

        // 倒序遍历合并区域（避免移除时索引变化）
        for (int i = mergedRegions.size() - 1; i >= 0; i--) {
            CellRangeAddress region = mergedRegions.get(i);
            if (region.isInRange(rowIndex, colIndex)) {
                // 1. 拆分合并区域
                sheet.removeMergedRegion(i);

                // 2. 获取原合并单元格的值和样式
                Row firstRow = sheet.getRow(region.getFirstRow());
                Cell firstCell = firstRow.getCell(region.getFirstColumn());
                CellStyle originalStyle = firstCell.getCellStyle();
                String cellValue = getCellValueAsString(firstCell);

                // 3. 填充拆分后的所有单元格
                for (int r = region.getFirstRow(); r <= region.getLastRow(); r++) {
                    Row row = sheet.getRow(r) != null ? sheet.getRow(r) : sheet.createRow(r);
                    for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
                        Cell cell = row.getCell(c) != null ? row.getCell(c) : row.createCell(c);
                        cell.setCellValue(cellValue);
                        cell.setCellStyle(originalStyle); // 保持原始样式
                    }
                }
                break; // 一个单元格只能属于一个合并区域
            }
        }
    }

    private static String getCellValueAsString(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default :
                return "";
        }
    }


    @Test
    void xlsxReadTest(){
        try {
            //创建工作簿对象
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(new FileInputStream("D:\\testdata\\昆山\\城市规划制图图例.xlsx"));
            //获取工作簿下sheet的个数
            int sheetNum = xssfWorkbook.getNumberOfSheets();
            System.out.println("该excel文件中总共有："+sheetNum+"个sheet");
            //遍历工作簿中的所有数据
            for(int i = 0;i<sheetNum;i++) {
                //读取第i个工作表
                System.out.println("读取第"+(i+1)+"个sheet");
                XSSFSheet sheet = xssfWorkbook.getSheetAt(i);
                //获取最后一行的num，即总行数。此处从0开始
                int maxRow = sheet.getLastRowNum();
                for (int row = 0; row <= maxRow; row++) {
                    int maxRol = sheet.getRow(row).getLastCellNum();
                    for (int rol = 0; rol < maxRol; rol++){
                        unmergeIfMerged(sheet, row, rol);
                    }
                }

                for (int row = 0; row <= maxRow; row++) {
                    //获取最后单元格num，即总单元格数 ***注意：此处从1开始计数***
                    int maxRol = sheet.getRow(row).getLastCellNum();
                    System.out.println("--------第" + row + "行的数据如下--------");
                    for (int rol = 0; rol < maxRol; rol++){
//                        System.out.println(getMergedRegion(sheet, row, rol));
                        System.out.print(sheet.getRow(row).getCell(rol) + "  ");
//                        unmergeIfMerged(sheet, row, rol);
                    }
                }
                System.out.print("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void dataTest(){
        String msg = "eJyrVipJTMpJ9UvMTVWyUgpydfHx9HNV0lFKSSxJ9MksLlGyiq5Wyk/KSk0uyUwBqjAzAEq6u/r7AtkB/j6R7v5+GhqGZuYmJnomlpbGRpYK5kaGFiZ6ZhZGxoZGOoYWxgYGekamRqYGRvhkDI3MjPUsjQ3MjUx1UE3DLYNiGi4ZTU2ge128PYDOfbJjzqPOpYYGzzdufr+n52nvdCDPDMJ5sXQ/slQPSMoIxFOqja0FAJFKWJY=";
        byte[] decode = Base64.getDecoder().decode(msg);
        msg = new String(ZLibUtil.decompress(decode));
        System.out.println(msg);
    }

    @Test
    void xlsxParseTest(){

    }

    public void test1(){
        String mdbFile = "E:\\A0205-A0208.mdb";

        Driver driver = ogr.GetDriverByName("PGeo");
        System.out.println(driver);

        DataSource ds = driver.Open(mdbFile);
        if (ds == null){
            System.out.println("打开数据源失败：" + mdbFile);
            return;
        }

        int layerCount = ds.GetLayerCount();
        System.out.println("数据源：" + mdbFile + "，图层数量：" + layerCount);

        for (int i = 0; i < layerCount; i++){
            Layer layer = ds.GetLayer(i);
            if (layer == null){
                continue;
            }

            System.out.println();
            System.out.println("================= 图层[" + i + "] =================");
            System.out.println("图层名称：" + layer.GetName());
            System.out.println("几何列名：" + layer.GetGeometryColumn());
            System.out.println("要素个数：" + layer.GetFeatureCount());

            SpatialReference spatialReference = layer.GetSpatialRef();
            if (spatialReference != null){
                System.out.println("空间参考：" + spatialReference.GetName());
            }

            // 字段定义
            FeatureDefn featureDefn = layer.GetLayerDefn();
            int fieldCount = featureDefn.GetFieldCount();
            StringBuilder fieldNames = new StringBuilder();
            for (int f = 0; f < fieldCount; f++){
                if (f > 0){
                    fieldNames.append(", ");
                }
                fieldNames.append(featureDefn.GetFieldDefn(f).GetName());
            }
            System.out.println("字段数：" + fieldCount + "，字段：" + fieldNames);

            // 遍历图层内所有要素
            Feature feature;
            int featureIndex = 0;
            while ((feature = layer.GetNextFeature()) != null){
                StringBuilder sb = new StringBuilder();
                sb.append("  [").append(featureIndex++).append("] FID=").append(feature.GetFID());
                for (int f = 0; f < fieldCount; f++){
                    FieldDefn fieldDefn = featureDefn.GetFieldDefn(f);
                    sb.append(" ").append(fieldDefn.GetName())
                            .append("=").append(feature.GetFieldAsString(f));
                }
                System.out.println(sb);

                Geometry geometry = feature.GetGeometryRef();
                if (geometry != null){
                    System.out.println("      几何类型：" + geometry.GetGeometryName());
                    System.out.println("      WKT：" + geometry.ExportToWkt());
                } else {
                    System.out.println("      几何类型：无");
                }

                feature.delete();
            }
        }

        ds.delete();
    }

    @Test
    void mdbReadTest(){
// 注册所有的驱动
        ogr.RegisterAll();
        // 为了支持中文路径，请添加下面这句代码
        gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8","YES");
        // 为了使属性表字段支持中文，请添加下面这句
        gdal.SetConfigOption("SHAPE_ENCODING","CP936");

        // mdb连接串
        gdal.SetConfigOption("PGEO_DRIVER_TEMPLATE", "DRIVER=Microsoft Access Driver (*.mdb, *.accdb);DBQ=%s");
        gdal.SetConfigOption("MDB_DRIVER_TEMPLATE", "DRIVER=Microsoft Access Driver (*.mdb, *.accdb);DBQ=%s");

        test1();

        gdal.GDALDestroyDriverManager();
    }

}
