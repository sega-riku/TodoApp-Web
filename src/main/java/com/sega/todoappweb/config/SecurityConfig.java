package com.sega.todoappweb.config;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http
    ) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                // Spring Bootのエラー処理を許可
                .dispatcherTypeMatchers(
                    DispatcherType.ERROR
                ).permitAll()

                // その他はログイン必須
                .anyRequest().authenticated()
            )

            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())

            .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}