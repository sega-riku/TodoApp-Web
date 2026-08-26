package com.sega.todoappweb.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sega.todoappweb.user.User;
import com.sega.todoappweb.user.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner createUser(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {

        return args -> {

            // 同じユーザーを毎回作らない
            if (userRepository.findByUsername("user1").isEmpty()) {

                User user = new User(
                    "user1",
                    "user1@example.com",
                    passwordEncoder.encode("password"),
                    "USER"
                );

                userRepository.save(user);
            }
        };
    }
}