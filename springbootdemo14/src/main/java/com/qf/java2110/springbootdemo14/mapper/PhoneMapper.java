package com.qf.java2110.springbootdemo14.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface PhoneMapper {

    @Select("select stock from t_phone where id = #{id}")
    public int getStockById(@Param("id") int id);

    @Update("update t_phone set stock = stock-1 where id=#{id}")
    int updateStockById(@Param("id") int id);
}
