package com.sega.todoappweb.passwordreset;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sega.todoappweb.mail.MailService;
import com.sega.todoappweb.user.User;
import com.sega.todoappweb.user.UserRepository;

@Controller
public class PasswordResetController {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final MailService mailService;

    public PasswordResetController(
        UserRepository userRepository,
        PasswordResetTokenRepository tokenRepository,
        MailService mailService,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
    }

    //パスワード再設定申請画面
    @GetMapping("/password/forgot")
    public String forgotPassword() {

        return "auth/forgotPassword";
    }

    //パスワード再設定申請処理
    @PostMapping("/password/forgot")
    public String forgotPassword(
        @RequestParam String email,
        Model model
    ) {

        email = email.trim();

        //メールアドレス未入力チェック
        if (email.isBlank()) {

            model.addAttribute(
                "emailError",
                "メールアドレスを入力してください"
            );

            return "auth/forgotPassword";
        }

        //メールアドレスからユーザー取得
        User user =
            userRepository
                .findByEmail(email)
                .orElse(null);

        //登録されていないメールアドレスの場合も、
        //アカウントの存在を外部に知らせないため
        //同じ完了メッセージを表示する
        if (user != null) {

            //ランダムなトークン生成
            String token =
                UUID.randomUUID().toString();

            //有効期限を30分後に設定
            LocalDateTime expiresAt =
                LocalDateTime.now()
                    .plusMinutes(30);

            //トークン情報作成
            PasswordResetToken resetToken =
                new PasswordResetToken(
                    user,
                    token,
                    expiresAt
                );

            //DB保存
            tokenRepository.save(
                resetToken
            );

            //再設定URL作成
            String resetUrl =
                "http://localhost:8080/password/reset?token="
                + token;

            //メール送信
            mailService.sendPasswordResetMail(
                user.getEmail(),
                resetUrl
            );
        }

        model.addAttribute(
            "mailSent",
            true
        );

        return "auth/forgotPassword";
    }

    //パスワード再設定画面
    @GetMapping("/password/reset")
    public String resetPassword(
        @RequestParam String token,
        Model model
    ) {

        //トークン取得
        PasswordResetToken resetToken =
            tokenRepository
                .findByToken(token)
                .orElse(null);

        //トークンが存在しない場合
        if (resetToken == null) {

            model.addAttribute(
                "tokenError",
                "パスワード再設定URLが無効です"
            );

            return "auth/resetPassword";
        }

        //使用済みの場合
        if (resetToken.isUsed()) {

            model.addAttribute(
                "tokenError",
                "このパスワード再設定URLは既に使用されています"
            );

            return "auth/resetPassword";
        }

        //有効期限切れの場合
        if (resetToken.isExpired()) {

            model.addAttribute(
                "tokenError",
                "パスワード再設定URLの有効期限が切れています"
            );

            return "auth/resetPassword";
        }

        //正常なトークンを画面へ渡す
        model.addAttribute(
            "token",
            token
        );

        return "auth/resetPassword";
    }

    //パスワード再設定処理
    @PostMapping("/password/reset")
    public String resetPassword(
        @RequestParam String token,
        @RequestParam String newPassword,
        @RequestParam String confirmPassword,
        Model model
    ) {

        //トークン取得
        PasswordResetToken resetToken =
            tokenRepository
                .findByToken(token)
                .orElse(null);

        //トークン存在チェック
        if (resetToken == null) {

            model.addAttribute(
                "tokenError",
                "パスワード再設定URLが無効です"
            );

            return "auth/resetPassword";
        }

        //使用済みチェック
        if (resetToken.isUsed()) {

            model.addAttribute(
                "tokenError",
                "このパスワード再設定URLは既に使用されています"
            );

            return "auth/resetPassword";
        }

        //有効期限チェック
        if (resetToken.isExpired()) {

            model.addAttribute(
                "tokenError",
                "パスワード再設定URLの有効期限が切れています"
            );

            return "auth/resetPassword";
        }

        //パスワード文字数チェック
        if (newPassword.length() < 8) {

            model.addAttribute(
                "passwordError",
                "パスワードは8文字以上で入力してください"
            );

            model.addAttribute(
                "token",
                token
            );

            return "auth/resetPassword";
        }

        //英字チェック
        if (!newPassword.matches(".*[A-Za-z].*")) {

            model.addAttribute(
                "passwordError",
                "パスワードには英字を1文字以上含めてください"
            );

            model.addAttribute(
                "token",
                token
            );

            return "auth/resetPassword";
        }

        //数字チェック
        if (!newPassword.matches(".*[0-9].*")) {

            model.addAttribute(
                "passwordError",
                "パスワードには数字を1文字以上含めてください"
            );

            model.addAttribute(
                "token",
                token
            );

            return "auth/resetPassword";
        }

        //確認パスワード一致チェック
        if (!newPassword.equals(confirmPassword)) {

            model.addAttribute(
                "passwordError",
                "パスワードが一致しません"
            );

            model.addAttribute(
                "token",
                token
            );

            return "auth/resetPassword";
        }

        //対象ユーザー取得
        User user =
            resetToken.getUser();

        //新しいパスワードを暗号化して保存
        user.setPassword(
            passwordEncoder.encode(
                newPassword
            )
        );

        userRepository.save(
            user
        );

        //トークン使用済み処理
        resetToken.use();

        tokenRepository.save(
            resetToken
        );

        return "redirect:/login?passwordReset";
    }
}