package com.hz.web.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * application-dynamic.yml 的读取类。
 *
 * <p>yml 顶层的 {@code tables} 节点下面按分类挂配置，两类都是「年份 -&gt; 明细」的结构，
 * 所以用 {@code @ConfigurationProperties(prefix = "tables")} + 两个字段来接：</p>
 *
 * <pre>
 * tables:
 *   kg:                        # 控规：年份 -&gt; {table, ftpDir}
 *     2024:
 *       "table":  "kg2024"
 *       "ftpDir": "kg/2024"
 *   dxt:                       # 地形图：年份 -&gt; 比例尺 -&gt; ftp 文件路径
 *     2024:
 *       "500":  "dxt/2024/500"
 *       "1000": "dxt/2024/1000"
 * </pre>
 *
 * <p><b>关于年份 / 比例尺的 key：一律写纯数字。</b> Spring Boot 把 Map 的 key 从属性名还原回来时
 * 会做一次名字归一化，非字母数字的字符会被丢掉，所以 {@code "1:500"} 绑出来会静默变成 {@code "1500"}
 * （不是报错，是数据被悄悄改了）。要展示成 {@code "1:500"} 请用 {@link #scaleLabel(String)}
 * 或 {@link #getDxtScales(String)} 补前缀。年份本身是纯数字，没有这个问题。</p>
 *
 * <p>以后再加新的分类，需要在下面补一个字段（各分类结构不同，没法用统一的泛型兜住）。</p>
 *
 * @author saber
 */
@Component
@Data
@Slf4j
@ConfigurationProperties(prefix = "tables")
public class DynamicTableConfig {

    /** 控规：年份 -> 明细。用 LinkedHashMap 保证跟 yml 里的书写顺序一致。 */
    private Map<String, KgItem> kg = new LinkedHashMap<>();

    /** 地形图：年份 -> 比例尺(纯数字) -> ftp 文件路径。 */
    private Map<String, Map<String, String>> dxt = new LinkedHashMap<>();

    // ==================== kg ====================

    /**
     * 按年份取 kg 的明细，找不到返回 null（拿返回值之后先判空再取属性）。
     */
    public KgItem getKgItem(String year) {
        return year == null ? null : kg.get(year);
    }

    /**
     * 按年份取 kg 的物理表名，找不到返回 null。
     */
    public String getKgTable(String year) {
        KgItem item = getKgItem(year);
        return item == null ? null : item.getTable();
    }

    /**
     * 按年份取 kg 的 ftp 目录，找不到返回 null。
     */
    public String getKgFtpDir(String year) {
        KgItem item = getKgItem(year);
        return item == null ? null : item.getFtpDir();
    }

    // ==================== dxt ====================

    /**
     * 取某一年下的全部比例尺，key 已还原成 {@code "1:500"} 这种展示形式，value 是配置里写的值。
     * 年份不存在时返回空 Map 而不是 null。
     */
    public Map<String, String> getDxtScales(String year) {
        Map<String, String> raw = year == null ? null : dxt.get(year);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> labeled = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            labeled.put(scaleLabel(entry.getKey()), entry.getValue());
        }
        return labeled;
    }

    /**
     * 取某年 + 某个比例尺对应的值。scale 传 {@code "500"} 或 {@code "1:500"} 都能命中，取不到返回 null。
     */
    public String getDxtPath(String year, String scale) {
        Map<String, String> raw = year == null ? null : dxt.get(year);
        if (raw == null || scale == null) {
            return null;
        }
        // 兼容调用方直接传 "1:500"
        String key = scale.startsWith("1:") ? scale.substring(2) : scale;
        return raw.get(key.trim());
    }

    /**
     * 把纯数字比例尺补成展示形式：{@code "500"} -&gt; {@code "1:500"}。
     * 已经是 {@code "1:"} 开头或为空时原样返回。
     */
    public static String scaleLabel(String scale) {
        if (scale == null || scale.isEmpty() || scale.startsWith("1:")) {
            return scale;
        }
        return "1:" + scale;
    }

    /**
     * 给接口和日志用的完整视图：结构跟 yml 一致，只是 dxt 的比例尺 key 补上了 {@code "1:"}。
     */
    public Map<String, Object> toViewMap() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("kg", kg);
        view.put("dxt", buildDxtView());
        return view;
    }

    /**
     * dxt 的展示结构：年份 -&gt; { {@code "1:500"} -&gt; 值 }。
     */
    public Map<String, Map<String, String>> buildDxtView() {
        Map<String, Map<String, String>> dxtView = new LinkedHashMap<>();
        for (String year : dxt.keySet()) {
            dxtView.put(year, getDxtScales(year));
        }
        return dxtView;
    }

    /**
     * 启动时打几行日志，方便直接从启动日志确认配置有没有被读到。
     * 做法和 {@code MyConstant#init()} 一致。
     */
    @PostConstruct
    public void logConfig() {
        if (kg.isEmpty() && dxt.isEmpty()) {
            log.warn("application-dynamic.yml 的 tables 节点没有绑定到任何配置，请检查配置文件路径与 spring.profiles.include");
            return;
        }
        log.info("dynamic tables [kg] 共 {} 年: {}", kg.size(), kg);
        for (Map.Entry<String, Map<String, String>> entry : buildDxtView().entrySet()) {
            log.info("dynamic tables [dxt.{}] 共 {} 条: {}", entry.getKey(),
                    entry.getValue().size(), entry.getValue());
        }
    }

    /**
     * 单条控规年度配置。
     */
    @Data
    public static class KgItem {
        /** kg 对应的物理表名 */
        private String table;
        /** kg 数据所在的 ftp 目录 */
        private String ftpDir;
    }
}
