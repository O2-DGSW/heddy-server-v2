package com.heddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HeddyApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeddyApplication.class, args);
    }
}
