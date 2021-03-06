package com.qf.java2110.springbootdemo14.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyRedissonConfig {

    /**
     * 所有对Redisson的使用都是通过RedissonClient对象
     */
    // 指定销毁对象的方法，服务停止时，调用这个方法进行销毁
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() {
        //1.创建配置    单redis节点模式
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        //2.根据配置 创建RedissonClient 实例
        return Redisson.create(config);
    }
}
