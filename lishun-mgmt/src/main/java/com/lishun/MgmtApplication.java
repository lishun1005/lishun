package com.lishun;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lishun.dao")
public class MgmtApplication {

	public static void main(String[] args) {
		SpringApplication.run(MgmtApplication.class, args);
	}
}
