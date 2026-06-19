package com.bankmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BankMindApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankMindApplication.class, args);
    }
}
