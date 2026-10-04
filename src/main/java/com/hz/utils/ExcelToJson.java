package com.hz.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelToJson {
    public static void main(String[] args) throws Exception {
        String xlsxPath = "E:\\ftp\\pipeCollisionStandard\\水平净距标准new.xlsx";
        JSONObject holeHStandardJson = ExcelToJson.readHoleStandardFromXlsx(xlsxPath);
        System.out.println(holeHStandardJson.toJSONString());
//
//        xlsxPath = "E:\\ftp\\pipeCollisionStandard\\垂直净距标准new.xlsx";
//        JSONObject holeVStandardJson = ExcelToJson.readHoleStandardFromXlsx(xlsxPath);
//        System.out.println(holeVStandardJson.toJSONString());
//
//        xlsxPath = "E:\\ftp\\pipeCollisionStandard\\管线大小类.xlsx";
//        JSONObject sbJson = ExcelToJson.readPipeSmallBigType(xlsxPath);
//        System.out.println(sbJson.toJSONString());
//
    }

    public static JSONObject readHoleStandardFromXlsx(String xlsxPath) throws Exception {
        JSONObject result = new JSONObject();

        int codeSheetNum = 0;
        JSONObject codeJson = ExcelToJson.readCodeFromXlsx(xlsxPath, codeSheetNum);
//        System.out.println(codeJson.toJSONString());

        int standardSheetNum = 1;
        JSONObject standardJson = ExcelToJson.readStandardFromXlsx(xlsxPath, standardSheetNum, codeJson);
//        System.out.println(standardJson.toJSONString());

        result.put("ids", codeJson.getJSONArray("ids"));
        result.put("values", standardJson.getJSONObject("values"));

        return result;
    }

    private static JSONObject readCodeFromXlsx(String xlsxPath, int sheetNum) throws Exception {
        FileInputStream fis = new FileInputStream(xlsxPath);
        Workbook wb = new XSSFWorkbook(fis);
        Sheet sheet = wb.getSheetAt(sheetNum);

        JSONObject root = new JSONObject();
        JSONArray ids = new JSONArray();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String id = getCellString(row.getCell(0));
            String category = getCellString(row.getCell(1));
            String condField = getCellString(row.getCell(2));
            String condAnd = getCellString(row.getCell(3));
            String condOr = getCellString(row.getCell(4));
            String subCondField = getCellString(row.getCell(5));

            if (id == null || category == null || category.isEmpty()) continue;

            JSONObject entry = new JSONObject();

            entry.put("id", id);
            entry.put("bigType", category);

//            JSONObject categoryObj = new JSONObject();
            JSONObject condition = new JSONObject();
            // 处理 condition.and / condition.or
            if (condAnd != null && !condAnd.isEmpty()) {
                condition.put("and", parseCondition(condAnd));
            }
            if (condOr != null && !condOr.isEmpty()) {
                condition.put("or", parseCondition(condOr));
            }
            entry.put("condition", condition);

            if (condField != null && !condField.isEmpty())
                entry.put("variable", condField);
            if (subCondField != null && !subCondField.isEmpty())
                entry.put("subVariable", subCondField);

//            entry.put(category, categoryObj);
            ids.add(entry);
        }

        root.put("ids", ids);

//        System.out.println(root.toJSONString());
        System.out.println("✅ 读取编码完成！");
        wb.close();
        fis.close();

        return root;
    }

    private static JSONObject readStandardFromXlsx(String xlsxPath, int sheetNum, JSONObject codeJson) throws Exception {
        FileInputStream fis = new FileInputStream(xlsxPath);
        Workbook wb = new XSSFWorkbook(fis);
        Sheet sheet = wb.getSheetAt(sheetNum);

        JSONObject root = new JSONObject();
        JSONObject values = new JSONObject();

        // 第一行是列id，从第2列开始
        Row header = sheet.getRow(0);
        int colCount = header.getLastCellNum();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String rowId = getCellString(row.getCell(0));
            if (rowId == null || rowId.isEmpty()) continue;

            for (int j = 1; j < colCount; j++) {
                Cell cell = row.getCell(j);
                if (cell == null) continue;

                String colId = getCellString(header.getCell(j));
                String value = getCellString(cell);

                if (colId == null || colId.isEmpty() || value == null || value.isEmpty()) continue;

                JSONObject obj = new JSONObject();
                obj.put("default_value", parseValue(value, rowId, codeJson));
                values.put(rowId + ":" + colId, obj);
            }
        }

        root.put("values", values);
        wb.close();
        fis.close();

        System.out.println("✅ 标准数值读取转换完成");

        return root;
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private static JSONArray parseCondition(String cond) {
        JSONArray arr = new JSONArray();
        for (String c : cond.split(",")) {
            arr.add(c.trim());
        }
        return arr;
    }

    public static JSONObject readPipeSmallBigType(String xlsxPath) throws Exception {
        FileInputStream fis = new FileInputStream(xlsxPath);
        Workbook wb = new XSSFWorkbook(fis);
        Sheet sheet = wb.getSheetAt(0);

        JSONObject result = new JSONObject();

        // 从第二行开始读取（假设第一行是表头）
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String key = getCellString(row.getCell(0)); // 第一列：大类
            String value = getCellString(row.getCell(1)); // 第二列：子类

            if (key == null || key.isEmpty() || value == null || value.isEmpty()) continue;

            result.put(key, value);
        }

        wb.close();
        fis.close();

        System.out.println("✅ 读取管线大小类完成");

        return result;
    }
    

    /**
     * 解析单元格值：
     * 1. 普通数字 => Double
     * 2. 带括号如 "<=300(0.4:0.5)" => 结构化解析
     */
    private static Object parseValue(String value, String rowId, JSONObject codeJson) throws Exception {
        value = value.trim();

        // 匹配类似 "<=300(0.4:0.5)" 的格式
        Pattern pattern = Pattern.compile("^(<=|>=|<|>|=)?\\s*([0-9.]+)\\s*\\(([^:]+):([^\\)]+)\\)$");
        Matcher matcher = pattern.matcher(value);

        if (matcher.find()) {
            // 找变量名
            String subVariableName = null;
            JSONArray jsonArrayIds = codeJson.getJSONArray("ids");
            for (Object o : jsonArrayIds) {
                // 转成 jsonObject
                JSONObject idObj = (JSONObject) o;
                String sId = idObj.getString("id");
                if (sId.equals(rowId)){
                    subVariableName = idObj.getString("subVariable");
                }
            }

//            String subVariableName = codeJson.getJSONObject("ids").getJSONObject(rowId).getString("subVariable");
            if (subVariableName == null || subVariableName.isEmpty()){
                // 抛异常
                throw new Exception("未找到Id: " + rowId + " 对应的变量名，是否未配置？");
            };

            JSONObject obj = new JSONObject();
            obj.put("condition", matcher.group(1) + matcher.group(2)); // <=300
            obj.put("variable", subVariableName);
            obj.put("condition_value", parseNumber(matcher.group(3)));
            obj.put("default_value", parseNumber(matcher.group(4)));
            return obj;
        }

        // 否则尝试解析为数字
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return value;
        }
    }

    private static Object parseNumber(String str) {
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {
            return str;
        }
    }
}
