package com.qf.java2110.springbootdemo14.service;


import com.qf.java2110.springbootdemo14.config.Result;
import com.qf.java2110.springbootdemo14.pojo.User;
import org.apache.ibatis.annotations.Lang;

import java.util.List;

public interface UserService {

    List<User> findUsers();

    List<User> findUsers2(String name);

    List<User> findUsers3();

    List<User> findUsers4();

    List<User> findUsers5();

    Result findUsers6(Lang id);

    Result update(User user);


}
