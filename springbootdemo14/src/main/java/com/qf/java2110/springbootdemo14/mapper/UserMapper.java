package com.qf.java2110.springbootdemo14.mapper;

import com.qf.java2110.springbootdemo14.pojo.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {


    List<User> findUser();

    List<User> finuser2();

    int update();

    User select();

    User selectId();
}
