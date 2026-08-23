package com.sega.todoappweb.user;

import java.security.Principal;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sega.todoappweb.admin.AdminNotification;
import com.sega.todoappweb.admin.AdminNotificationRepository;
import com.sega.todoappweb.task.TaskRepository;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminNotificationRepository adminNotificationRepository;
    private final TaskRepository taskRepository;

    public UserController(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AdminNotificationRepository adminNotificationRepository,
        TaskRepository taskRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminNotificationRepository = adminNotificationRepository;
        this.taskRepository = taskRepository;
    }

    //ユーザー情報変更処理
    @PostMapping("/user/update")
    public String updateUser(
        @RequestParam(required = false) String newUsername,
        @RequestParam(required = false) String currentPassword,
        @RequestParam(required = false) String newPassword,
        @RequestParam(required = false) String confirmPassword,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {

        //ログインユーザー取得処理
        User user =
            userRepository
                .findByUsername(principal.getName())
                .orElseThrow();

        //ユーザー名整形処理
        if (newUsername != null) {
            newUsername = newUsername.trim();
        }

        //ユーザー名変更有無判定
        boolean usernameChanged =
            newUsername != null
            && !newUsername.isBlank()
            && !newUsername.equals(user.getUsername());

        //パスワード変更有無判定
        boolean passwordChanged =
            newPassword != null
            && !newPassword.isBlank();

        //変更内容未入力チェック
        if (!usernameChanged && !passwordChanged) {

            redirectAttributes.addFlashAttribute(
                "userUpdateError",
                "変更する内容を入力してください"
            );

            //入力したユーザー名のみ保持
            keepUsername(
                redirectAttributes,
                newUsername
            );

            return "redirect:/";
        }

        //ユーザー名変更チェック
        if (usernameChanged) {

            //ユーザー名重複チェック
            if (userRepository.findByUsername(newUsername).isPresent()) {

                redirectAttributes.addFlashAttribute(
                    "userUpdateError",
                    "このユーザー名は既に使用されています"
                );

                //入力したユーザー名のみ保持
                keepUsername(
                    redirectAttributes,
                    newUsername
                );

                return "redirect:/";
            }
        }

        //パスワード変更チェック
        if (passwordChanged) {

            //現在のパスワード未入力チェック
            if (
                currentPassword == null
                || currentPassword.isBlank()
            ) {

                redirectAttributes.addFlashAttribute(
                    "userUpdateError",
                    "現在のパスワードを入力してください"
                );

                //入力したユーザー名のみ保持
                keepUsername(
                    redirectAttributes,
                    newUsername
                );

                return "redirect:/";
            }

            //現在のパスワード確認
            if (!passwordEncoder.matches(
                currentPassword,
                user.getPassword()
            )) {

                redirectAttributes.addFlashAttribute(
                    "userUpdateError",
                    "現在のパスワードが正しくありません"
                );

                //入力したユーザー名のみ保持
                keepUsername(
                    redirectAttributes,
                    newUsername
                );

                return "redirect:/";
            }

            //パスワード文字数チェック
            if (newPassword.length() < 8) {

                redirectAttributes.addFlashAttribute(
                    "userUpdateError",
                    "新しいパスワードは8文字以上で入力してください"
                );

                //入力したユーザー名のみ保持
                keepUsername(
                    redirectAttributes,
                    newUsername
                );

                return "redirect:/";
            }

            //パスワード英字チェック
            if (!newPassword.matches(".*[A-Za-z].*")) {

                redirectAttributes.addFlashAttribute(
                    "userUpdateError",
                    "新しいパスワードには英字を1文字以上含めてください"
                );

                //入力したユーザー名のみ保持
                keepUsername(
                    redirectAttributes,
                    newUsername
                );

                return "redirect:/";
            }

            //パスワード数字チェック
            if (!newPassword.matches(".*[0-9].*")) {

                redirectAttributes.addFlashAttribute(
                    "userUpdateError",
                    "新しいパスワードには数字を1文字以上含めてください"
                );

                //入力したユーザー名のみ保持
                keepUsername(
                    redirectAttributes,
                    newUsername
                );

                return "redirect:/";
            }

            //確認パスワード一致チェック
            if (
                confirmPassword == null
                || !newPassword.equals(confirmPassword)
            ) {

                redirectAttributes.addFlashAttribute(
                    "userUpdateError",
                    "新しいパスワードが一致しません"
                );

                //入力したユーザー名のみ保持
                keepUsername(
                    redirectAttributes,
                    newUsername
                );

                return "redirect:/";
            }
        }

        //ユーザー名変更処理
        if (usernameChanged) {
            user.setUsername(newUsername);
        }

        //パスワード変更処理
        if (passwordChanged) {

            user.setPassword(
                passwordEncoder.encode(newPassword)
            );
        }

        //ユーザー情報保存処理
        userRepository.save(user);

        //管理者向け履歴登録処理
        AdminNotification notification =
            new AdminNotification(
                user.getUsername()
                + "さんがユーザー情報を変更しました。"
            );

        adminNotificationRepository.save(notification);

        //ユーザー名変更後の認証情報更新処理
        if (usernameChanged) {

            Authentication currentAuthentication =
                SecurityContextHolder
                    .getContext()
                    .getAuthentication();

            Authentication newAuthentication =
                new UsernamePasswordAuthenticationToken(
                    user.getUsername(),
                    currentAuthentication.getCredentials(),
                    currentAuthentication.getAuthorities()
                );

            SecurityContextHolder
                .getContext()
                .setAuthentication(newAuthentication);
        }

        //変更成功メッセージ
        redirectAttributes.addFlashAttribute(
            "userUpdateSuccess",
            "ユーザー情報を変更しました"
        );

        return "redirect:/";
    }

    //アカウント削除処理
    @Transactional
    @PostMapping("/user/delete")
    public String deleteAccount(
        @RequestParam(required = false) String deletePassword,
        Principal principal,
        RedirectAttributes redirectAttributes,
        HttpServletRequest request
    ) {

        //ログインユーザー取得処理
        User user =
            userRepository
                .findByUsername(principal.getName())
                .orElseThrow();

        //現在のパスワード未入力チェック
        if (
            deletePassword == null
            || deletePassword.isBlank()
        ) {

            redirectAttributes.addFlashAttribute(
                "accountDeleteError",
                "現在のパスワードを入力してください"
            );

            return "redirect:/";
        }

        //現在のパスワード確認
        if (!passwordEncoder.matches(
            deletePassword,
            user.getPassword()
        )) {

            redirectAttributes.addFlashAttribute(
                "accountDeleteError",
                "現在のパスワードが正しくありません"
            );

            return "redirect:/";
        }

        //削除前ユーザー名保持
        String deletedUsername =
            user.getUsername();

        //ユーザーに紐づくタスク一括削除処理
        taskRepository.deleteByUser(user);

        //ユーザー削除処理
        userRepository.delete(user);

        //管理者向け履歴登録処理
        AdminNotification notification =
            new AdminNotification(
                deletedUsername
                + "さんがアカウントを削除しました。"
            );

        adminNotificationRepository.save(notification);

        //ログイン情報削除処理
        SecurityContextHolder.clearContext();

        //セッション破棄処理
        request.getSession().invalidate();

        return "redirect:/login?accountDeleted";
    }

    //エラー時ユーザー名保持処理
    private void keepUsername(
        RedirectAttributes redirectAttributes,
        String newUsername
    ) {

        if (
            newUsername != null
            && !newUsername.isBlank()
        ) {

            redirectAttributes.addFlashAttribute(
                "enteredNewUsername",
                newUsername
            );
        }
    }
}