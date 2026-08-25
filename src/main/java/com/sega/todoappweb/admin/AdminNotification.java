package com.sega.todoappweb.admin;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_notifications")
public class AdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private AdminNotificationType notificationType;

    public AdminNotification() {
    }

    //通常のお知らせ作成処理
    public AdminNotification(String message) {
        this.message = message;
        this.createdAt = LocalDateTime.now();
        this.notificationType = AdminNotificationType.NORMAL;
    }

    //種類指定のお知らせ作成処理
    public AdminNotification(
        String message,
        AdminNotificationType notificationType
    ) {
        this.message = message;
        this.createdAt = LocalDateTime.now();
        this.notificationType = notificationType;
    }

    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public AdminNotificationType getNotificationType() {
        return notificationType;
    }
}