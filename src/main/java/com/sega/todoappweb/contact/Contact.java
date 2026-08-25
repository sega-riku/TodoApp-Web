package com.sega.todoappweb.contact;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

//お問い合わせ情報を保持するクラス
@Entity
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;

    @Enumerated(EnumType.STRING)
    private ContactType contactType;

    private String content;

    @Enumerated(EnumType.STRING)
    private ContactStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String response;
    private String reply;
    private String repliedBy;
    private LocalDateTime repliedAt;
    private boolean replyRead;

    public Contact() {
    }

    //引数ありのコンストラクタ
    public Contact(
        String username,
        ContactType contactType,
        String content
    ) {
        this.username = username;
        this.contactType = contactType;
        this.content = content;
        this.status = ContactStatus.UNHANDLED;
        this.createdAt = LocalDateTime.now();
        this.completedAt = null;
        this.response = null;
        this.reply = null;
        this.repliedBy = null;
        this.repliedAt = null;
        this.replyRead = false;
    }

    //getter
    public Long getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public ContactType getContactType() {
        return this.contactType;
    }

    public String getContent() {
        return this.content;
    }

    public ContactStatus getStatus() {
        return this.status;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return this.completedAt;
    }

    public String getResponse() {
        return this.response;
    }

    public String getReply() {
        return this.reply;
    }

    public String getRepliedBy() {
        return this.repliedBy;
    }

    public LocalDateTime getRepliedAt() {
        return this.repliedAt;
    }

    public boolean isReplyRead() {
        return this.replyRead;
    }

    //setter
    public void setStatus(ContactStatus status) {

        this.status = status;

        //対応済みに変更した場合
        if (status == ContactStatus.COMPLETED) {

            this.completedAt =
                LocalDateTime.now();

        } else {

            //未対応・対応中に戻した場合
            this.completedAt = null;
        }
    }

    public void setResponse(String response) {
        this.response = response;
    }

    //ユーザーへの返信設定処理
    public void reply(
        String reply,
        String repliedBy
    ) {
        this.reply = reply;
        this.repliedBy = repliedBy;
        this.repliedAt = LocalDateTime.now();
        this.replyRead = false;
    }

    //返信確認済み処理
    public void readReply() {
        this.replyRead = true;
    }
}