package com.sega.todoappweb;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

//一件分のタスク情報を保持するクラス

public class Task {
    private String title; 
    private LocalDate deadline;
    private LocalTime time;
    private DateType dateType;
    private boolean completed;

    //引数ありのコンストラクタ
    public Task(String title, LocalDate deadline, LocalTime time, DateType dateType){  
        this.title = title; //タスク名設定
        this.deadline = deadline; //締切呼び出し
        this.time = time;
        this.dateType = dateType;
        this.completed = false; //完了ステータスを「未完了」と設定
    }

    public Task(String title, LocalDate deadline, LocalTime time, DateType dateType, boolean completed){
        this.title = title; 
        this.deadline = deadline;
        this.time = time;
        this.dateType = dateType;
        this.completed = completed;
    }

    //getter
    public String getTitle(){ //タイトル取得
        return this.title;
    }
    public LocalDate getDeadline(){ //予定・締切日取得
        return this.deadline;
    }
    public LocalTime getTime(){ // 予定・締切時間取得
        return this.time;
    }
    public DateType getDateType(){
        return this.dateType;
    }
    public boolean isCompleted(){ //完了ステータス取得
        return this.completed;
    }

    public boolean isExpired(){

        LocalDateTime taskDateTime = LocalDateTime.of(this.deadline, time);

        return this.dateType == DateType.DEADLINE && taskDateTime.isBefore(LocalDateTime.now());
    }

    //setter
    public void setTitle(String title) { //タスク名を変更
        this.title = title;
    }
    public void setDeadline(LocalDate deadline) { //予定・締切日を変更
        this.deadline = deadline;
    }
    public void setTime(LocalTime time){
        this.time = time;
    }
    public void setDateType(DateType dateType){
        this.dateType = dateType;
    }
    public void complete(){ //ステータスを完了にする
        this.completed = true;
    }
    public void incomplete(){ //ステータスを未完了に戻す
        this.completed = false;
    }
}
