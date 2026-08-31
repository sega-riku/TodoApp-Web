package com.sega.todoappweb.mail;

import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.admin-email}")
    private String adminEmail;

    public MailService(
        JavaMailSender mailSender
    ) {
        this.mailSender = mailSender;
    }

    //パスワード再設定メール送信処理
    public void sendPasswordResetMail(
        String toEmail,
        String resetUrl
    ) {

        try {

            MimeMessage message =
                mailSender.createMimeMessage();

            MimeMessageHelper helper =
                new MimeMessageHelper(
                    message,
                    false,
                    "UTF-8"
                );

            //送信元表示名
            helper.setFrom(
                fromEmail,
                "Tascale"
            );

            helper.setTo(
                toEmail
            );

            helper.setSubject(
                "【Tascale】パスワード再設定のお知らせ"
            );

            String html =
                """
                <div style="font-family: sans-serif; line-height: 1.7;">

                    <h2>Tascale</h2>

                    <p>
                        Tascaleをご利用いただきありがとうございます。
                    </p>

                    <p>
                        パスワードの再設定が申請されました。<br>
                        以下のボタンから新しいパスワードを設定してください。
                    </p>

                    <p style="margin: 30px 0;">
                        <a href="%s"
                           style="
                               display: inline-block;
                               padding: 12px 24px;
                               background-color: #0d6efd;
                               color: white;
                               text-decoration: none;
                               border-radius: 6px;
                           ">
                            パスワードを再設定する
                        </a>
                    </p>

                    <p>
                        このURLの有効期限は30分です。
                    </p>

                    <p>
                        心当たりがない場合は、
                        このメールを無視してください。
                    </p>

                    <hr>

                    <p style="font-size: 0.9rem; color: #666;">
                        このメールはTascaleから自動送信されています。
                    </p>

                </div>
                """.formatted(
                    resetUrl
                );

            //true = HTMLとして送信
            helper.setText(
                html,
                true
            );

            mailSender.send(
                message
            );

        } catch (
            MessagingException
            | UnsupportedEncodingException e
        ) {

            throw new RuntimeException(
                "メールの送信に失敗しました",
                e
            );
        }
    }

    //管理者向けお問い合わせ通知メール送信処理
    @Async
    public void sendContactNotificationMail(
        String username,
        String contactType
    ) {

        try {

            MimeMessage message =
                mailSender.createMimeMessage();

            MimeMessageHelper helper =
                new MimeMessageHelper(
                    message,
                    false,
                    "UTF-8"
                );

            //送信元表示名
            helper.setFrom(
                fromEmail,
                "Tascale"
            );

            //管理者メールアドレス
            helper.setTo(
                adminEmail
            );

            helper.setSubject(
                "【Tascale】新しいお問い合わせ"
            );

            String html =
                """
                <div style="font-family: sans-serif; line-height: 1.7;">

                    <h2>Tascale</h2>

                    <p>
                        新しいお問い合わせが届きました。
                    </p>

                    <p>
                        <strong>ユーザー名：</strong>%s<br>
                        <strong>お問い合わせ種別：</strong>%s
                    </p>

                    <p>
                        詳細はTascaleの管理画面からご確認ください。
                    </p>

                    <hr>

                    <p style="font-size: 0.9rem; color: #666;">
                        このメールはTascaleから自動送信されています。
                    </p>

                </div>
                """.formatted(
                    username,
                    contactType
                );

            //true = HTMLとして送信
            helper.setText(
                html,
                true
            );

            mailSender.send(
                message
            );

        } catch (
            MessagingException
            | UnsupportedEncodingException e
        ) {

            throw new RuntimeException(
                "お問い合わせ通知メールの送信に失敗しました",
                e
            );
        }
    }

    //ユーザー向けお問い合わせ返信通知メール送信処理
    @Async
    public void sendContactReplyNotificationMail(
        String toEmail,
        String username
    ) {

        try {

            MimeMessage message =
                mailSender.createMimeMessage();

            MimeMessageHelper helper =
                new MimeMessageHelper(
                    message,
                    false,
                    "UTF-8"
                );

            //送信元表示名
            helper.setFrom(
                fromEmail,
                "Tascale"
            );

            //ユーザーのメールアドレス
            helper.setTo(
                toEmail
            );

            helper.setSubject(
                "【Tascale】お問い合わせへの返信が届きました"
            );

            String html =
                """
                <div style="font-family: sans-serif; line-height: 1.7;">

                    <h2>Tascale</h2>

                    <p>
                        %sさん
                    </p>

                    <p>
                        お問い合わせへの返信が届きました。
                    </p>

                    <p>
                        Tascaleにログインし、<br>
                        「お問い合わせ履歴」から返信内容をご確認ください。
                    </p>

                    <hr>

                    <p style="font-size: 0.9rem; color: #666;">
                        このメールはTascaleから自動送信されています。
                    </p>

                </div>
                """.formatted(
                    username
                );

            //true = HTMLとして送信
            helper.setText(
                html,
                true
            );

            mailSender.send(
                message
            );

        } catch (
            MessagingException
            | UnsupportedEncodingException e
        ) {

            throw new RuntimeException(
                "お問い合わせ返信通知メールの送信に失敗しました",
                e
            );
        }
    }
    //ユーザー向けお知らせメール送信処理
    @Async
    public void sendAnnouncementMail(
        String toEmail,
        String subject,
        String content
    ){
        try{
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,false,"UTF-8");

            //送信元表示名
            helper.setFrom(fromEmail,"Tascale");

            //ユーザーのメールアドレス
            helper.setTo(toEmail);

            helper.setSubject("【Tascale】" + subject);

            String html =
                """
                <div stylr="font-family: sans-serif; line-height: 1.7;">
                    <h2>Tascale</h2>

                    <p>
                        Tascale運営からのお知らせです。
                    </p>

                    <p style="white-space: pre-wrap;">%s</p>

                    <hr>

                    <p style="font-size: 0.9rem; color: #666;">
                        このメールはTascaleから送信されています。
                    </p>
                </div>
                        """.formatted(content);

            helper.setText(html,true);
            mailSender.send(message);
        }catch(MessagingException | UnsupportedEncodingException e){
            throw new RuntimeException("お知らせメールの送信に失敗しました", e);
        }
    }
}