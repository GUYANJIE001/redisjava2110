package com.qf.java2110.springbootdemo14.service.impl;

import com.qf.java2110.springbootdemo14.mapper.PhoneMapper;
import com.qf.java2110.springbootdemo14.service.PhoneService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.Time;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 总的来说，一个分布式锁要可用满足
 * 1. 互斥性 setnx
 * 2. 避免死锁 设置过期时间
 * 3. 避免规则 判断
 * 4. 加锁，解锁得有原子性 lua脚本
 */
@Service("phoneService")
public class PhoneServiceImpl implements PhoneService {

    @Resource
    private PhoneMapper phoneMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public int getStockById(int id) {
        return phoneMapper.getStockById(id);
    }

    @Override
    public int updateStockById(int id) {
        return phoneMapper.updateStockById(id);
    }

    // 手动实现分布式分离
    @Override
    public String buyPhone3() {

        int stock = this.getStockById(1);
        if (stock <= 0) {
            return "sorry";
        }
        //占坑 加锁
        //Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("mylock", "phonelock");   //setnx
        //第二个优化    在业务执行过程中 如果没有来得及释放锁 会出现死锁 所以 给 锁设置过期时间  尽可能避免出现死锁
        //给锁设置过期时间
        //stringRedisTemplate.expire("mylock",10,TimeUnit.SECONDS);

        //加锁和设置过期时间 需要具备原子性   因为在设置过期时间之前 也有可能出现死锁
        //Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("mylock","phonelock",3,TimeUnit.SECONDS);

        //第三个优化   防误删问题
        // 比如锁的过期时间是3秒   业务执行时间是7秒 ， 就会出现 第一个线程  把第三个线程的锁解了的情况  所以 设置一个 lockValue
        //删锁之前  进行判断
        String lockKey = "mylock";
        String lockValue = UUID.randomUUID().toString();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(lockKey,lockValue,3, TimeUnit.SECONDS);

        try {
            if (lock) {
                stock = this.getStockById(1);
                if (stock > 0) {
                    TimeUnit.SECONDS.sleep(2);
                    int result = this.updateStockById(1);
                    return "恭喜你买到了";
                } else {
                    return "sorry";
                }
            } else {
                // 第一个优化
                // 第一个进来的线程，占有了分布式锁，后续线程在下面的while循环中，一直尝试获取锁

                // 自旋锁
                while (true) {
                    //lock = stringRedisTemplate.opsForValue().setIfAbsent("mylock","phonelock");
                    lock = stringRedisTemplate.opsForValue().setIfAbsent(lockKey,lockValue,3,TimeUnit.SECONDS);
                    if (lock) {
                        stock = this.getStockById(1);
                        if (stock > 0) {

                            TimeUnit.SECONDS.sleep(2);

                            int result = this.updateStockById(1);
                            return "恭喜你买到了";
                        } else {
                            return "sorry";
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //解锁
            //stringRedisTemplate.delete("mylock");

            //防误删 解锁之前 判断一下 解的是不是自己的锁
            //if(lockValue.equals(stringRedisTemplate.opsForValue().get(lockKey))){
            //
            //    stringRedisTemplate.delete(lockKey);
            //}
            //上面删除的操作不具备原子性  得优化

            //使用  lua 脚本
            //定义  lua 脚本
            String script = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
            // 使用redis执行lua脚本，封装到DefaultRedisScript对象中
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);

            stringRedisTemplate.execute(redisScript, Arrays.asList(lockKey),lockValue);
        }
        return "sorry";
    }

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public String buyPhone4() {

        int stock = this.getStockById(1);
        if(stock<=0){
            return "sorry";
        }

        RLock lock = redissonClient.getLock("redisson-lock");
        //lock.lock();
        lock.lock(15,TimeUnit.SECONDS);
        try {
            stock = this.getStockById(1);
            if(stock>0){

                TimeUnit.SECONDS.sleep(20);

                int result = this.updateStockById(1);
                return "恭喜你买到了";
            }else{
                return "sorry";
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

        return "sorry";
    }
}
