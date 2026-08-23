package com.sega.todoappweb.task;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

import com.sega.todoappweb.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

//一件分のタスク情報を保持するクラス
@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private LocalDate deadline;
    private LocalTime time;
    private DateType dateType;
    private boolean completed;
    private String description;

    //タスク所有ユーザー
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Task() {
    }

    //引数ありのコンストラクタ
    public Task(
        String title,
        LocalDate deadline,
        LocalTime time,
        DateType dateType,
        String description
    ) {
        this.title = title; //タスク名設定
        this.deadline = deadline; //締切呼び出し
        this.time = time;
        this.dateType = dateType;
        this.completed = false; //完了ステータスを「未完了」と設定
        this.description = description;
    }

    public Task(
        String title,
        LocalDate deadline,
        LocalTime time,
        DateType dateType,
        boolean completed,
        String description
    ) {
        this.title = title;
        this.deadline = deadline;
        this.time = time;
        this.dateType = dateType;
        this.completed = completed;
        this.description = description;
    }

    //getter
    public Long getId() {
        return this.id;
    }

    public String getTitle() { //タイトル取得
        return this.title;
    }

    public LocalDate getDeadline() { //予定・締切日取得
        return this.deadline;
    }

    public LocalTime getTime() { //予定・締切時間取得
        return this.time;
    }

    public DateType getDateType() {
        return this.dateType;
    }

    public boolean isCompleted() { //完了ステータス取得
        return this.completed;
    }

    public boolean isExpired() {
        LocalDateTime taskDateTime =
            LocalDateTime.of(this.deadline, time);

        return this.dateType == DateType.DEADLINE
            && taskDateTime.isBefore(LocalDateTime.now());
    }

    public String getDescription() {
        return this.description;
    }

    public User getUser() {
        return this.user;
    }

    //setter
    public void setTitle(String title) { //タスク名を変更
        this.title = title;
    }

    public void setDeadline(LocalDate deadline) { //予定・締切日を変更
        this.deadline = deadline;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public void setDateType(DateType dateType) {
        this.dateType = dateType;
    }

    public void complete() { //ステータスを完了にする
        this.completed = true;
    }

    public void incomplete() { //ステータスを未完了に戻す
        this.completed = false;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUser(User user) {
        this.user = user;
    }
}