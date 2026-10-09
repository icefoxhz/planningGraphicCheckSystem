package com.hz.web.config;

import com.hz.web.service.TableMetadataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动钩子：Redis 探活 + 空间表初始化。
 *
 * @author saber
 */
@Component
@Slf4j
public class StartupDbTask implements ApplicationRunner {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    TableMetadataService tableMetadataService;

    @Value("${myProject.cache.strategy}")
    private String strategy;

    @Value("${myProject.cache.is_start_init}")
    private boolean isStartInit;

    @Override
    public void run(ApplicationArguments args) {
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
        }

        log.info(">>>>>> 启动完成 <<<<<<");
    }
}
