package com.qf.java2110.springbootdemo14.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.Alias;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Alias("user")
public class User implements Serializable {

    private Long id;
    private String name;
    private String password;

}
