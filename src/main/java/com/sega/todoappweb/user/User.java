package com.sega.todoappweb.user;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String email;
    private String password;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private String homeLayout;

    // JPA用
    public User() {
    }

    public User(String username,String email, String password, String role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdAt = LocalDateTime.now();
        this.lastLoginAt = null;
        this.homeLayout = "SCHEDULE_LEFT";
    }

    //getter
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail(){
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRole(){
        return role;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public String getHomeLayout(){
        return homeLayout;
    }

    //setter
    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(String role){
        this.role = role;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void setHomeLayout(String homeLayout){
        this.homeLayout = homeLayout;
    }
}