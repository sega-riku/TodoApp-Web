package com.sega.todoappweb.user;

import java.security.Principal;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sega.todoappweb.admin.AdminNotification;
import com.sega.todoappweb.admin.AdminNotificationRepository;

@Controller
public class LoginController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminNotificationRepository adminNotificationRepository;

    public LoginController(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AdminNotificationRepository adminNotificationRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminNotificationRepository = adminNotificationRepository;
    }

    // ログイン処理
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    // 登録画面
    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

    // 登録処理
    @PostMapping("/register")
    public String register(
        @RequestParam String username,
        @RequestParam String password,
        @RequestParam String confirmPassword,
        Model model
    ) {

        if (username == null || username.isBlank()) {

            model.addAttribute(
                "usernameError",
                "ユーザー名を入力してください"
            );

            return "auth/register";
        }

        username = username.trim();

        model.addAttribute(
            "enteredUsername",
            username
        );

        if (userRepository.findByUsername(username).isPresent()) {

            model.addAttribute(
                "usernameDuplicateError",
                "このユーザー名は既に使用されています"
            );

            return "auth/register";
        }

        if (password.length() < 8) {

            model.addAttribute(
                "passwordLengthError",
                "パスワードは8文字以上で入力してください"
            );

            return "auth/register";
        }

        if (!password.matches(".*[A-Za-z].*")) {

            model.addAttribute(
                "passwordLetterError",
                "パスワードには英字を1文字以上含めてください"
            );

            return "auth/register";
        }

        if (!password.matches(".*[0-9].*")) {

            model.addAttribute(
                "passwordNumberError",
                "パスワードには数字を1文字以上含めてください"
            );

            return "auth/register";
        }

        if (!password.equals(confirmPassword)) {

            model.addAttribute(
                "passwordError",
                "パスワードが一致しません"
            );

            return "auth/register";
        }

        User user = new User(
            username,
            passwordEncoder.encode(password),
            "USER"
        );

        userRepository.save(user);

        AdminNotification notification = new AdminNotification(user.getUsername() + "さんが新規登録しました。");

        adminNotificationRepository.save(notification);

        return "redirect:/login?registered";
    }

    // パスワード変更画面
    @GetMapping("/password/change")
    public String changePassword() {
        return "auth/changePassword";
    }

    // パスワード変更処理
    @PostMapping("/password/change")
    public String changePassword(
        @RequestParam String currentPassword,
        @RequestParam String newPassword,
        @RequestParam String confirmPassword,
        Principal principal,
        Model model
    ) {

        User user = userRepository
            .findByUsername(principal.getName())
            .orElseThrow();

        // 現在のパスワード確認
        if (!passwordEncoder.matches(
            currentPassword,
            user.getPassword()
        )) {

            model.addAttribute(
                "currentPasswordError",
                "現在のパスワードが正しくありません"
            );

            return "auth/changePassword";
        }

        // 8文字以上か確認
        if (newPassword.length() < 8) {

            model.addAttribute(
                "passwordLengthError",
                "新しいパスワードは8文字以上で入力してください"
            );

            return "auth/changePassword";
        }

        // 英字が含まれているか確認
        if (!newPassword.matches(".*[A-Za-z].*")) {

            model.addAttribute(
                "passwordLetterError",
                "新しいパスワードには英字を1文字以上含めてください"
            );

            return "auth/changePassword";
        }

        // 数字が含まれているか確認
        if (!newPassword.matches(".*[0-9].*")) {

            model.addAttribute(
                "passwordNumberError",
                "新しいパスワードには数字を1文字以上含めてください"
            );

            return "auth/changePassword";
        }

        // 確認用パスワードと一致するか確認
        if (!newPassword.equals(confirmPassword)) {

            model.addAttribute(
                "passwordError",
                "新しいパスワードが一致しません"
            );

            return "auth/changePassword";
        }

        // 新しいパスワードを暗号化して保存
        user.setPassword(
            passwordEncoder.encode(newPassword)
        );

        userRepository.save(user);

        // 管理者向け履歴を保存
        AdminNotification notification =
            new AdminNotification(
                user.getUsername()
                + "さんがパスワードを変更しました。"
            );

        adminNotificationRepository.save(notification);

        return "redirect:/?passwordChanged";
    }
}