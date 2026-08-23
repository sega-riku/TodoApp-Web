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
            //アクセス権限設定
            .authorizeHttpRequests(auth -> auth

                //Spring Bootのエラー処理を許可
                .dispatcherTypeMatchers(
                    DispatcherType.ERROR
                ).permitAll()

                //未ログインでもアクセス可能
                .requestMatchers(
                    "/login",
                    "/register"
                ).permitAll()

                //管理者のみアクセス可能
                .requestMatchers("/admin/**")
                .hasRole("ADMIN")

                //その他はログイン必須
                .anyRequest()
                .authenticated()
            )

            //ログイン設定
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )

            //ログアウト設定
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )

            //アクセス権限不足時の処理
            .exceptionHandling(exception -> exception

                //403エラーの場合Todo画面へ戻す
                .accessDeniedHandler(
                    (request, response, accessDeniedException) ->
                        response.sendRedirect("/")
                )
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}