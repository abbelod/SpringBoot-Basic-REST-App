package com.example.lecture02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class Lecture02Application {

    public static void main(String[] args) {
        SpringApplication.run(Lecture02Application.class, args);
    }

}
