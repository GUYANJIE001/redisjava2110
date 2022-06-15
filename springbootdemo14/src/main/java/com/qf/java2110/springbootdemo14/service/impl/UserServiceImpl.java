package com.qf.java2110.springbootdemo14.service.impl;

import com.alibaba.fastjson.JSON;
import com.qf.java2110.springbootdemo14.Cache;
import com.qf.java2110.springbootdemo14.config.Result;
import com.qf.java2110.springbootdemo14.mapper.UserMapper;
import com.qf.java2110.springbootdemo14.pojo.User;
import com.qf.java2110.springbootdemo14.service.UserService;
import com.qf.java2110.springbootdemo14.util.RedisUtil;
import jodd.util.StringUtil;
import org.apache.ibatis.annotations.Lang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service("userService")
public class UserServiceImpl implements UserService {

    private static final String CACHE_SHOP_KEY = "CACHE_SHOP_KEY";
    private static final Long CACHE_SHOP_TTL = 60L;
    @Resource
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private Cache cache;

    // 查询数据库数据
    @Override
    public List<User> findUsers() {
        return userMapper.findUser();
    }

    // 查redis
    @Override
    public List<User> findUsers2(String name) {
        // 先从redis取数据，如果redis中数据，就表示缓存命中，则直接返回redis中的数据
        List<User> userList = (List<User>) redisTemplate.opsForValue().get("name");

        if (userList != null) {
            // 缓存命中
            System.out.println("缓存命中，不用查询数据库");
            return userList;
        } else {
            // 缓存没有命中
            System.out.println("缓存没有命中，需要查询数据库");
            userList = userMapper.findUser();
            // 把查询出来的数据库放入redis中，下次从redis取数据
            redisTemplate.opsForValue().set("name",userList);
        }

        return userList;
    }

    @Override
    public List<User> findUsers3() {

        String userList = stringRedisTemplate.opsForValue().get("userList");

        if (userList != null) {
            // 缓存命中
            System.out.println("缓存命中，不用查询数据库");
            List<User> users = JSON.parseArray(userList,User.class);

            return users;
        } else {
            // 单线程模式锁
            System.out.println("进锁了");
            userList = stringRedisTemplate.opsForValue().get("userList");
            if (userList != null) {
                List<User> users = JSON.parseArray(userList,User.class);
                return users;

            }

            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("缓存没有命中，查询数据库");
            List<User> users = userMapper.findUser();

            stringRedisTemplate.opsForValue().set("userList",JSON.toJSONString(users));
            return users;
        }
    }

    @Override
    public List<User> findUsers4() {
        String userList = stringRedisTemplate.opsForValue().get("userList");

        if (userList != null) {
            // 缓存命中
            System.out.println("缓存命中，不用查询数据库");
            List<User> users = JSON.parseArray(userList,User.class);

            return users;
        } else {
            Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock","111");

            // 让线程休眠5秒
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (!lock) {
                userList = stringRedisTemplate.opsForValue().get("userList");

                if (userList!=null) {
                    List<User> users = JSON.parseArray(userList,User.class);
                    return users;
                }
            }

            System.out.println("缓存没有命中，查询数据库");
            List<User> users = userMapper.findUser();

            stringRedisTemplate.opsForValue().set("userList",JSON.toJSONString(users));
            return users;

        }
    }

    @Override
    public List<User> findUsers5() {

        // 先从redis中取数据，如果redis中有数据，就表示缓存命中，则直接返回redis中的数据
        String userList = stringRedisTemplate.opsForValue().get("userList");

        // 判断取出的数据是否为空
        if (userList != null) {
            // 如果不为空就表示缓存命中
            System.out.println("缓存命中，不用查数据库");
            List<User> user = JSON.parseArray(userList,User.class);
            return user;
        } else {
            // 如果为空就表示缓存里边没有数据，需要在数据库里边查到数据并添加到redis缓存中
            System.out.println("缓存没有命中");
            List<User> list = userMapper.findUser();

            // 将查询出来的数据放入到redis中，下次从redis中取数据
            stringRedisTemplate.opsForValue().set("userList",JSON.toJSONString(list));
            return list;
        }
    }

    // 使用redis缓存查找用户信息
    @Override
    public Result findUsers6(Lang id) {
        // 根据id从redis查询数据
        String key = CACHE_SHOP_KEY;
        String user = stringRedisTemplate.opsForValue().get(key);

        // 查到数据库真实数据，直接返回数据
        if (StringUtil.isNotBlank(user)) {
            User user1 = JSON.parseObject(user,User.class);
            return Result.success();
        }

        // 查不到，根据id查询数据库
        User user1 = userMapper.select();
        if (user1 == null) {
            // 数据库查不到
            return Result.success(false,"id不存在");
        }

        // 数据库查到，将数据写入到redis缓存，返回相关数据，设置超时时间
        stringRedisTemplate.opsForValue().set(key,JSON.toJSONString(user1),CACHE_SHOP_TTL,TimeUnit.MINUTES);
        return Result.success(user);
    }

    // 更新缓存操作,先更新数据库，再删除缓存
    @Override
    public Result update(User user) {
       String key = CACHE_SHOP_KEY;
       User user1 = userMapper.selectId();
        if (user1 == null) {
            return Result.success(false,"id不能为空");
        }
        // 更新数据库
        userMapper.update();
        // 查询数据
        User user2 = userMapper.selectId();
        // 删除缓存
        stringRedisTemplate.delete(key);
        // 将更新好的数据添加到redis
        stringRedisTemplate.opsForValue().set(key,JSON.toJSONString(user2),CACHE_SHOP_TTL,TimeUnit.MINUTES);
        return Result.success(user);
    }
}
