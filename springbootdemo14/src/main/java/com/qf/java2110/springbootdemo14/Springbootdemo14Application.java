package com.qf.java2110.springbootdemo14;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.qf.java2110.springbootdemo14.mapper")
public class Springbootdemo14Application {

	public static void main(String[] args) {
		SpringApplication.run(Springbootdemo14Application.class, args);
	}

}
