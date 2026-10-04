package com.hz.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FieldValueGenerateByRuleUtil {

    static String parseYearMonthDay(String front) {
        switch (front) {
            case "年":
                return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
            case "年月":
                return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            case "年月日":
                return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        return null;
    }

     static String padNumber(String numberStr, int length) {
        if (numberStr == null) {
            throw new IllegalArgumentException("numberStr 不能为 null");
        }
        return String.format("%0" + length + "d", Integer.parseInt(numberStr));
    }

    static String parseNum(String keyPart, String back) {
        String key = null;
        int len = 0;

        switch (back) {
            case "三位流水号":
                key = keyPart + "_3";
                len = 3;
                break;
            case "四位流水号":
                key = keyPart + "_4";
                len = 4;
                break;
            case "五位流水号":
                key = keyPart + "_5";
                len = 5;
                break;
            case "六位流水号":
                key = keyPart + "_6";
                len = 6;
                break;
            case "七位流水号":
                key = keyPart + "_7";
                len = 7;
                break;
            case "八位流水号":
                key = keyPart + "_8";
                len = 8;
                break;
        }
        if (key == null) {
            return null;
        }

        int next = MultiKeySequenceUtil.next(key);
        return padNumber(String.valueOf(next), len);
    }

    public static String generateValue(String layerName, String fieldName, String rule) throws Exception {
        String[] split = rule.split("\\+");
        if (split.length != 3) {
            throw new Exception("规则不符号要求! rule = " + rule);
        }

        String start = split[0];
        String front = split[1];
        String back = split[2];

        String front_parse = parseYearMonthDay(front);

        String keyPart = layerName.toLowerCase() + "_" + fieldName.toLowerCase();
        String back_parse = parseNum(keyPart, back);

        if (front_parse == null || back_parse == null) {
            throw new Exception("规则不符号要求! rule = " + rule);
        }

        return start + front_parse + back_parse;
    }

//    public static void main(String[] args) throws Exception {
//        System.out.println(FieldValueGenerateByRuleUtil.generateValue("readline", "f1", "DK+年月+五位流水号"));
//    }
}
