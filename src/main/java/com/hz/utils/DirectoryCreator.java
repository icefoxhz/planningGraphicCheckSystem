package com.hz.utils;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class DirectoryCreator {

    // 每个目录路径对应一个锁对象
    private static final ConcurrentHashMap<String, Object> dirLocks = new ConcurrentHashMap<>();

    public static void createDirectory(String path) {
        File dir = new File(path);
        if (dir.exists()) {
            return;
        }

        // 获取锁对象（如果不存在则放入）
        Object lock = dirLocks.computeIfAbsent(path, k -> new Object());
        synchronized (lock) {
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created && !dir.exists()) {
                    throw new RuntimeException("Failed to create directory: " + path);
                }
            }
        }

        // 可选：清理无用锁对象（避免内存泄露）
        dirLocks.remove(path, lock);
    }
}
