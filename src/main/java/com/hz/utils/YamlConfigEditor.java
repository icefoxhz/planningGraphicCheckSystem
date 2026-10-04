package com.hz.utils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author saber
 */
public class YamlConfigEditor {
//    public static void main(String[] args) throws IOException {
//        // 1. 创建工具类实例（传入 application.yml 的路径）
//        YamlConfigEditor editor = new YamlConfigEditor("config/application-dynamic.yml");
//
//        // 2. 读取值
//        System.out.println("原来的 login.retryTimes: " + editor.get("login.retryTimes"));
//
//        // 3. 修改值
//        editor.set("login.retryCount", 10);
//
//        // 4. 保存到文件
//        editor.save();
//
//        System.out.println("修改完成");
//
//        // 5. 读取值
//        System.out.println("现在的 login.retryTimes: " + editor.get("login.retryTimes"));
//    }

    private final String filePath;
    private Map<String, Object> data;
    private final Yaml yaml;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public YamlConfigEditor(String filePath) throws IOException {
        this.filePath = filePath;
        this.yaml = new Yaml();
        load();
    }

    /** 读取 YAML 文件到 Map */
    @SuppressWarnings("unchecked")
    public void load() throws IOException {
        lock.writeLock().lock(); // 加写锁，防止同时读写
        try (InputStream in = Files.newInputStream(Paths.get(filePath))) {
            this.data = yaml.load(in);
            if (this.data == null) {
                this.data = new LinkedHashMap<>();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** 获取值（支持多层key：spring.datasource.url） */
    public Object get(String keyPath) {
        lock.readLock().lock();
        try {
            String[] keys = keyPath.split("\\.");
            Map<String, Object> current = data;
            Object value = null;
            for (String key : keys) {
                value = current.get(key);
                if (value instanceof Map) {
                    current = (Map<String, Object>) value;
                }
            }
            return value;
        } finally {
            lock.readLock().unlock();
        }
    }

    /** 设置值（支持多层key：spring.datasource.url） */
    @SuppressWarnings("unchecked")
    public void set(String keyPath, Object value) {
        lock.writeLock().lock();
        try {
            String[] keys = keyPath.split("\\.");
            Map<String, Object> current = data;
            for (int i = 0; i < keys.length - 1; i++) {
                current = (Map<String, Object>) current.computeIfAbsent(keys[i], k -> new LinkedHashMap<>());
            }
            current.put(keys[keys.length - 1], value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** 保存到文件 */
    public void save() throws IOException {
        lock.writeLock().lock();
        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml yamlWriter = new Yaml(options);
            try (Writer writer = new FileWriter(filePath)) {
                yamlWriter.dump(data, writer);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** 获取全部数据 */
    public Map<String, Object> getAll() {
        lock.readLock().lock();
        try {
            return new LinkedHashMap<>(this.data); // 返回副本，防止外部修改
        } finally {
            lock.readLock().unlock();
        }
    }
}
