package com.hz.web.service;

import com.hz.constant.MyConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * @author saber
 */
@Service
public class RedisStorageService {
    @Autowired
    private RedisTemplate<String, Object> redisObjTemplate;

    @Autowired
    private StringRedisTemplate redisStringTemplate;

    public void setGeomBytes(String outerKey, Integer key, Object value) {
        // outerKey 是 表名， key 是objectId，value 是geomBytes
        redisObjTemplate.opsForHash().put(outerKey, String.valueOf(key), value);
    }

    public Map<Object, Object> getOuterKeyMap(String outerKey) {
        return redisObjTemplate.opsForHash().entries(outerKey);
    }

    public Object getGeomBytes(String outerKey, Integer key) {
        return redisObjTemplate.opsForHash().get(outerKey, key.toString());
    }

    public void setUserToken(String key, String value) {
        //outerKey = "userToken" , key 是username， value 是 token
//        redisStringTemplate.opsForHash().put(MyConstant.userTokenOuterKeyName, key, value);
        redisStringTemplate.opsForValue().set(MyConstant.USER_TOKEN_OUTER_KEY_NAME + key, value, Duration.ofHours(MyConstant.USER_TOKEN_EXPIRE_HOURS));

    }

    public Object getUserToken(String key) {
//        return redisStringTemplate.opsForHash().get(MyConstant.userTokenOuterKeyName, key);
        return redisStringTemplate.opsForValue().get(MyConstant.USER_TOKEN_OUTER_KEY_NAME + key);
    }
}
