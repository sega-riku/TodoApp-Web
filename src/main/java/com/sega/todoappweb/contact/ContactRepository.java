package com.sega.todoappweb.contact;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    //ステータス別お問い合わせ取得処理
    List<Contact> findByStatus(
        ContactStatus status
    );

    //指定ステータス以外のお問い合わせ取得処理
    List<Contact> findByStatusNot(
        ContactStatus status
    );

    //指定日時より後のお問い合わせ取得処理
    List<Contact> findByCreatedAtAfter(
        LocalDateTime createdAt
    );

    //ユーザー別お問い合わせ履歴取得処理
    List<Contact> findByUsernameOrderByCreatedAtDesc(
        String username
    );

    //ユーザー本人のお問い合わせ取得処理
    Optional<Contact> findByIdAndUsername(
        Long id,
        String username
    );

    //ユーザー別未読返信件数取得処理
    long countByUsernameAndReplyReadFalseAndReplyIsNotNull(
        String username
    );
}