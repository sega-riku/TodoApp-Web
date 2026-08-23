package com.sega.todoappweb.admin;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.security.Principal;

import com.sega.todoappweb.user.User;
import com.sega.todoappweb.user.UserRepository;

@Controller
public class AdminController {

    private final UserRepository userRepository;
    private final AdminNotificationRepository adminNotificationRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(
        UserRepository userRepository,
        AdminNotificationRepository adminNotificationRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.adminNotificationRepository = adminNotificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    //管理者画面
    @GetMapping("/admin")
    public String admin(
        Model model,
        Principal principal
    ) {

        List<User> users = userRepository.findAll();

        model.addAttribute(
            "users",
            users
        );

        model.addAttribute(
            "username",
            principal.getName()
        );

        long totalUsers = users.size();

        //管理者数カウント処理
        long adminCount = users.stream()
            .filter(user -> "ADMIN".equals(user.getRole()))
            .count();

        //ユーザー数カウント処理
        long userCount = users.stream()
            .filter(user -> "USER".equals(user.getRole()))
            .count();

        model.addAttribute(
            "totalUsers",
            totalUsers
        );

        model.addAttribute(
            "adminCount",
            adminCount
        );

        model.addAttribute(
            "userCount",
            userCount
        );

        //管理者向けお知らせ取得処理
        List<AdminNotification> notifications =
            adminNotificationRepository.findAll();

        notifications.sort(
            Comparator.comparing(
                AdminNotification::getCreatedAt
            ).reversed()
        );

        //最新5件まで表示
        if (notifications.size() > 5) {

            notifications =
                new ArrayList<>(
                    notifications.subList(0, 5)
                );
        }

        model.addAttribute(
            "notifications",
            notifications
        );

        return "admin/admin";
    }

    //ユーザー詳細画面処理
    @GetMapping("/admin/users/{id}")
    public String userDetail(
        @PathVariable Long id,
        Model model,
        Principal principal
    ) {

        //ユーザー取得処理
        User user = userRepository
            .findById(id)
            .orElse(null);

        //存在しないユーザーIDの場合は管理画面へ戻す
        if (user == null) {
            return "redirect:/admin";
        }

        model.addAttribute(
            "user",
            user
        );

        model.addAttribute(
            "username",
            principal.getName()
        );

        return "admin/adminUserDetail";
    }

    //ユーザー削除処理
    @GetMapping("/admin/users/delete/{id}")
    public String deleteUser(
        @PathVariable Long id,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {

        //削除対象ユーザー取得処理
        User user = userRepository
            .findById(id)
            .orElse(null);

        //存在しないユーザーIDの場合は管理画面へ戻す
        if (user == null) {
            return "redirect:/admin";
        }

        //ログイン中の管理者自身は削除しない
        if (user.getUsername().equals(principal.getName())) {

            redirectAttributes.addFlashAttribute(
                "selfDeleteError",
                "管理者としてログインしているため、このアカウントは削除できません。"
            );

            return "redirect:/admin/users/" + id;
        }

        //ユーザー削除処理
        userRepository.deleteById(id);

        return "redirect:/admin";
    }

    //管理者追加画面
    @GetMapping("/admin/register")
    public String adminRegister() {
        return "admin/adminRegister";
    }

    //管理者追加処理
    @PostMapping("/admin/register")
    public String adminRegister(
        @RequestParam String username,
        @RequestParam String password,
        @RequestParam String confirmPassword,
        Model model
    ) {

        //ユーザー名空白チェック
        if (username == null || username.isBlank()) {

            model.addAttribute(
                "usernameError",
                "ユーザー名を入力してください"
            );

            return "admin/adminRegister";
        }

        username = username.trim();

        model.addAttribute(
            "enteredUsername",
            username
        );

        //ユーザー名重複チェック
        if (userRepository.findByUsername(username).isPresent()) {

            model.addAttribute(
                "usernameDuplicateError",
                "このユーザー名は既に使用されています"
            );

            return "admin/adminRegister";
        }

        //パスワード文字数チェック
        if (password.length() < 8) {

            model.addAttribute(
                "passwordLengthError",
                "パスワードは8文字以上で入力してください"
            );

            return "admin/adminRegister";
        }

        //パスワード英字チェック
        if (!password.matches(".*[A-Za-z].*")) {

            model.addAttribute(
                "passwordLetterError",
                "パスワードには英字を1文字以上含めてください"
            );

            return "admin/adminRegister";
        }

        //パスワード数字チェック
        if (!password.matches(".*[0-9].*")) {

            model.addAttribute(
                "passwordNumberError",
                "パスワードには数字を1文字以上含めてください"
            );

            return "admin/adminRegister";
        }

        //パスワード一致チェック
        if (!password.equals(confirmPassword)) {

            model.addAttribute(
                "passwordError",
                "パスワードが一致しません"
            );

            return "admin/adminRegister";
        }

        //管理者ユーザー登録処理
        User user = new User(
            username,
            passwordEncoder.encode(password),
            "ADMIN"
        );

        userRepository.save(user);

        //管理者向けお知らせ登録処理
        AdminNotification notification =
            new AdminNotification(
                user.getUsername()
                + "さんが管理者として登録されました。"
            );

        adminNotificationRepository.save(notification);

        return "redirect:/admin";
    }
}