package com.qf.java2110.springbootdemo14.controller;

import com.qf.java2110.springbootdemo14.config.Result;
import com.qf.java2110.springbootdemo14.pojo.User;
import com.qf.java2110.springbootdemo14.service.PhoneService;
import com.qf.java2110.springbootdemo14.service.UserService;
import org.apache.ibatis.annotations.Lang;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.CredentialNotFoundException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@RestController
@RequestMapping("/redis")
public class RedisController {

    @Autowired
    private UserService userService;

    @RequestMapping("/UserList")
    public List<User> getUserList() {

        // 直接查询数据库
        return userService.findUsers();
    }

    @RequestMapping("/UserList2")
    public List<User> getUserList2(String name) {
        // 先从redis取数据，如果redis没有则从数据库中取数据，渠道之后放入redis
        return userService.findUsers2(name);
    }

    @RequestMapping("/UserList3")
    public List<User> getUserList3() {
        // 先从redis取数据，如果redis没有，则从数据库中取数据，渠道之后放入redis
        // 使用 stringRedisTemplate
        return userService.findUsers3();
    }

    @RequestMapping("/UserList4")
    public List<User> getUserList4() {
        // 先从redis取数据，如果redis没有则从数据库中取数据，渠道之后放入redis
        // 使用 stringRedisTemplate
        // 本地锁，无法在分布式场景下，保证线程安全问题
        return userService.findUsers4();
    }

    @RequestMapping("/UserList5")
    public List<User> getUserList5() {
        // 先从redis取数据，如果redis没有则从数据库中取数据，渠道之后放入redis
        return userService.findUsers5();
    }

    @RequestMapping("/UserList6")
    public Result getUserList6(Lang id) {
        // 先从redis中取数据，如果redis没有则从数据库中取数据，取到之后放入到redis中
        userService.findUsers6(id);
        return Result.success();
    }

    @RequestMapping("/UserList7")
    public Result getUserList7(User user) {
        // 先从redis中取数据，如果redis没有则从数据库中取数据，取到之后放入到redis中
        userService.update(user);
        return Result.success();
    }

    @Autowired
    private PhoneService phoneService;

    // 两个线程同时进入，会使两个线程都能抢到
    @RequestMapping("/buyPhone")
    public String buyPhone() {
        // 使用本地锁，只能对当前进程生效，如果是分布式场景，有多个服务，就会出现分布式锁
        // 先查询商品库存
        int stock = phoneService.getStockById(1);

        if (stock > 0) {
            synchronized (this) {
                stock = phoneService.getStockById(1);
                if (stock > 0) {
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    int result = phoneService.updateStockById(1);
                    return "恭喜你，买到了";
                }
            }
        }

        return "sorry 已售罄";
    }

    ReentrantLock lock = new ReentrantLock();

    // 使用两个线程会发生，两个线程都能抢到的问题
    @RequestMapping("/buyPhone2")
    public String buyPhone2() {
        // 先查看商品库存
        int stock = phoneService.getStockById(1);

        if (stock <= 0) {
            return "sorry,已售罄";
        }

        lock.lock();
        try {

            stock = phoneService.getStockById(1);
            if (stock > 0) {
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                int result = phoneService.updateStockById(1);
                return "恭喜你买到了";
            } else {
                return "sorry,已售罄";
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

        return "sorry,已售罄";
    }

    // 分布式锁，借用redis，setnx占坑思想
    // 手动实现分布式锁
    @RequestMapping("/buyPhone3")
    public String buyPhone3() {
        return phoneService.buyPhone3();
    }

    //使用  redisson 提供的分布式锁解决方案
    @RequestMapping("/buyPhone4")
    public String buyPhone4() {

        return phoneService.buyPhone4();
    }
    
    @Autowired
    private RedissonClient redisson;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    // 读写锁
    @GetMapping("/write")
    @ResponseBody
    public String writeValue() {
        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");

        String s="";

        RLock rLock = lock.writeLock();
        try {
            // 改数据  加写锁    读数据 加 读锁
            rLock.lock();
            s = UUID.randomUUID().toString();
            TimeUnit.SECONDS.sleep(15);
            redisTemplate.opsForValue().set("writeValue",s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            rLock.unlock();
        }

        return s;
    }

    // 读锁
    @GetMapping("/read")
    @ResponseBody
    public String readValue() {
        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");

        RLock rLock = lock.readLock();
        String value="";
        try{
            rLock.lock();  // 加 读锁

            value = redisTemplate.opsForValue().get("writeValue");

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            rLock.unlock();
        }


        return value;
    }


    /**
     * 放假 锁学校大门
     *
     * 5个班全部走完  我们可以锁大门
     * */

    @GetMapping("/lockDoor")
    @ResponseBody
    public String  lockDoor() throws InterruptedException {
        RCountDownLatch door = redisson.getCountDownLatch("door");

        door.trySetCount(5);   // 5个班

        door.await();  //  等待闭锁都完成

        return "放假了";
    }

    @GetMapping("/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") Long id){

        RCountDownLatch door = redisson.getCountDownLatch("door");

        door.countDown();   // 计数减一

        return id+"班的人都走了";
    }


    /**
     * 车位停车
     * 3车位

     *  利用信号量的特性   可以进行   限流操作
     *  比如 系统只能供10000 个线程访问  ，就可以分配10000 个 信号量
     *
     * */
    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        //先在 redis设置键值：   set park 3
        RSemaphore park = redisson.getSemaphore("park");

        // park.acquire();  // 获取一个信号 获取一个值 占一个车位    可以执行三次  车位占完  就没法继续往下执行了   需要释放

        boolean b = park.tryAcquire();   // 尝试 获取信号量   获取到返回 true  获取不到  返回false
        // 限流操作
        if(b){
            // 车位够  执行业务
        }else{
            // 车位不够  执行限流

        }
        return "ok" + b;

    }

    @GetMapping("/go")
    @ResponseBody
    public String go(){
        RSemaphore park = redisson.getSemaphore("park");

        park.release();   // 释放一个车位

        return "ok";
    }
}
