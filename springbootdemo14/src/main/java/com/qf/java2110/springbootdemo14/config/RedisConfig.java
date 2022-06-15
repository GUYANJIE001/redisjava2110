package com.qf.java2110.springbootdemo14.config;


import com.qf.java2110.springbootdemo14.Cache;
import com.qf.java2110.springbootdemo14.util.RedisCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @program: sailing
 * @description: Redis的配置类
 * @author: 谷延杰
 * @create: 2022-01-06 16:58
 **/
@Configuration
public class RedisConfig {

    @Bean
    public Cache cache(StringRedisTemplate redisTemplate) {
        return new RedisCache(redisTemplate);
    }
}
