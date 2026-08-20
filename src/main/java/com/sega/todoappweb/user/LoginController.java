package com.sega.todoappweb.user;

import java.security.Principal;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginController(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    //ログイン処理
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    //登録処理
    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

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

        return "redirect:/login?registered";
    }

    //パスワード変更処理
    @GetMapping("/password/change")
    public String changePassword() {
        return "auth/changePassword";
    }

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

        if (newPassword.length() < 8) {
            model.addAttribute(
                "passwordLengthError",
                "新しいパスワードは8文字以上で入力してください"
            );

            return "auth/changePassword";
        }

        if (!newPassword.matches(".*[A-Za-z].*")) {
            model.addAttribute(
                "passwordLetterError",
                "新しいパスワードには英字を1文字以上含めてください"
            );

            return "auth/changePassword";
        }

        if (!newPassword.matches(".*[0-9].*")) {
            model.addAttribute(
                "passwordNumberError",
                "新しいパスワードには数字を1文字以上含めてください"
            );

            return "auth/changePassword";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute(
                "passwordError",
                "新しいパスワードが一致しません"
            );

            return "auth/changePassword";
        }

        user.setPassword(
            passwordEncoder.encode(newPassword)
        );

        userRepository.save(user);

        return "redirect:/?passwordChanged";
    }
}