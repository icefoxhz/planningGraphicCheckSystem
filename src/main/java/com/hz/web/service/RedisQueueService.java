package com.hz.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author saber
 */
@Service
public class RedisQueueService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    public void deleteQueue(String queueKey){
        redisTemplate.delete(queueKey);
    }

    public void pushMessage(String queueKey, String message) {
        redisTemplate.opsForList().rightPush(queueKey, message);
    }

    public void pushMessage(String queueKey, String message, long timeout, TimeUnit unit) {
        redisTemplate.opsForList().rightPush(queueKey, message);
        redisTemplate.expire(queueKey, timeout, unit);
    }


    public String blockPopMessage(String queueKey, long timeout, TimeUnit unit){
        return redisTemplate.opsForList().leftPop(queueKey, timeout, unit);
    }

    public String popMessage(String queueKey){
        return redisTemplate.opsForList().leftPop(queueKey);
    }

    /**
     * 批量获取队列
     */
    public List<String> getMessage(String queueKey, int start, int end) {
        return redisTemplate.opsForList().range(queueKey, start, end);
    }

    /**
     * 获取队列
     */
    public List<String> getAll(String queueKey) {
        return redisTemplate.opsForList().range(queueKey, 0, -1);
    }

    /**
     * 获取队列长度
     */
    public long getQueueSize(String queueKey) {
        Long size = redisTemplate.opsForList().size(queueKey);
        return size != null ? size : 0;
    }

    public void expireQueue(String queueKey, long timeout, TimeUnit unit) {
        redisTemplate.expire(queueKey, timeout, unit);
    }

}
