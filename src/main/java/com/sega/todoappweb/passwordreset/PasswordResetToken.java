package com.sega.todoappweb.passwordreset;

import java.time.LocalDateTime;

import com.sega.todoappweb.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //再設定対象ユーザー
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    //再設定用トークン
    private String token;

    //有効期限
    private LocalDateTime expiresAt;

    //使用済みかどうか
    private boolean used;

    //JPA用
    public PasswordResetToken() {
    }

    //引数ありコンストラクタ
    public PasswordResetToken(
        User user,
        String token,
        LocalDateTime expiresAt
    ) {
        this.user = user;
        this.token = token;
        this.expiresAt = expiresAt;
        this.used = false;
    }

    //getter
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    //使用済みにする処理
    public void use() {
        this.used = true;
    }

    //有効期限切れ判定
    public boolean isExpired() {
        return LocalDateTime.now()
            .isAfter(expiresAt);
    }
}