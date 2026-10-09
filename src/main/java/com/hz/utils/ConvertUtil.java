package com.hz.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author saber
 */
@Slf4j
public class ConvertUtil {
    //    // 定义日期格式
//    public static DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//    public static DateTimeFormatter formatterDateCn = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final ObjectMapper objectMapper = new ObjectMapper();


    public static String encodeMd5(String s) {
        String md5 = null;
        try {

            md5 = DigestUtils.md5DigestAsHex(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("MD5加密失败", e);
        }
        return md5;
    }

    public static Map objectToMap(Object obj) {
        return objectMapper.convertValue(obj, Map.class);
    }

    public static List<Map<String, Object>> objectToList(Object obj) {
        if (obj == null) {
            return Collections.emptyList();
        }
        return objectMapper.convertValue(obj, new TypeReference<List<Map<String, Object>>>() {});
    }

    public static Map<String, Object> convertKeysToUpper(Map<String, Object> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toUpperCase(),
                        Map.Entry::getValue
                ));
    }

    /**
     * 全角转半角
     * @param input 待转换的字符串
     * @return 转换后的字符串
     */
    public static String toHalfWidth(String input) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            // 全角空格 12288 转半角空格 32
            if (c == 12288) {
                sb.append(' ');
            }
            // 其他全角字符（！～） 转半角
            else if (c >= 65281 && c <= 65374) {
                sb.append((char)(c - 65248));
            }
            else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
