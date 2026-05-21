package com.untitled;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.untitled.mapper")
@EnableScheduling
public class UntitledApplication {
    public static void main(String[] args) {
        SpringApplication.run(UntitledApplication.class, args);
    }
}
