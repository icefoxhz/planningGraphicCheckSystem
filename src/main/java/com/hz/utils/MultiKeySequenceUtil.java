package com.hz.utils;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

public class MultiKeySequenceUtil {
    private static final String SEQ_FILE_PATH = "sequence.properties";
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Properties properties = new Properties();

    // 初始化加载属性文件
    static {
        File file = new File(SEQ_FILE_PATH);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("无法创建序列文件", e);
            }
        }

        try (FileInputStream fis = new FileInputStream(SEQ_FILE_PATH)) {
            properties.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("加载序列文件失败", e);
        }
    }

    public static int next(String key) {
        lock.lock();
        try {
            int current = Integer.parseInt(properties.getProperty(key, "0"));
            int next = current + 1;
            properties.setProperty(key, String.valueOf(next));
            persist(); // 保存到文件
            return next;
        } finally {
            lock.unlock();
        }
    }

    private static void persist() {
        try (FileOutputStream fos = new FileOutputStream(SEQ_FILE_PATH)) {
            properties.store(fos, "Multi-key Sequence");
        } catch (IOException e) {
            throw new RuntimeException("写入序列文件失败", e);
        }
    }

    // 示例
    public static void main(String[] args) {
        System.out.println("order -> " + MultiKeySequenceUtil.next("order"));
        System.out.println("invoice -> " + MultiKeySequenceUtil.next("invoice"));
        System.out.println("order -> " + MultiKeySequenceUtil.next("order"));
    }
}
