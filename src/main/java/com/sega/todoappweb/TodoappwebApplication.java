package com.sega.todoappweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TodoappwebApplication {

    public static void main(String[] args) {
        SpringApplication.run(TodoappwebApplication.class, args);
    }

}