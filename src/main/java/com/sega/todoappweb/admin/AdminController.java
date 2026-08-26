package com.sega.todoappweb.admin;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.security.Principal;

import com.sega.todoappweb.user.User;
import com.sega.todoappweb.user.UserRepository;
import com.sega.todoappweb.task.TaskRepository;
import com.sega.todoappweb.contact.Contact;
import com.sega.todoappweb.contact.ContactRepository;
import com.sega.todoappweb.contact.ContactStatus;
import com.sega.todoappweb.mail.MailService;

@Controller
public class AdminController {

    private final UserRepository userRepository;
    private final AdminNotificationRepository adminNotificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final TaskRepository taskRepository;
    private final ContactRepository contactRepository;
    private final MailService mailService;

    public AdminController(
        UserRepository userRepository,
        AdminNotificationRepository adminNotificationRepository,
        PasswordEncoder passwordEncoder,
        TaskRepository taskRepository,
        ContactRepository contactRepository,
        MailService mailService
    ) {
        this.userRepository = userRepository;
        this.adminNotificationRepository = adminNotificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.taskRepository = taskRepository;
        this.contactRepository = contactRepository;
        this.mailService = mailService;
    }

    //管理者画面
    @GetMapping("/admin")
    public String admin(
        @RequestParam(required = false) Boolean newContact,
        Model model,
        Principal principal
    ) {

        List<User> users = userRepository.findAll();

        model.addAttribute("users", users);
        model.addAttribute("username", principal.getName());

        long totalUsers = users.size();

        //管理者数カウント処理
        long adminCount =
            users.stream()
                .filter(
                    user ->
                        "ADMIN".equals(
                            user.getRole()
                        )
                )
                .count();

        //ユーザー数カウント処理
        long userCount =
            users.stream()
                .filter(
                    user ->
                        "USER".equals(
                            user.getRole()
                        )
                )
                .count();

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("userCount", userCount);

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

        //未対応・対応中お問い合わせ取得処理
        List<Contact> contacts =
            contactRepository.findByStatusNot(
                ContactStatus.COMPLETED
            );

        //お問い合わせ日降順処理
        contacts.sort(
            Comparator.comparing(
                Contact::getCreatedAt
            ).reversed()
        );

        model.addAttribute(
            "contacts",
            contacts
        );

        //新しいお問い合わせ通知判定
        model.addAttribute(
            "newContact",
            Boolean.TRUE.equals(
                newContact
            )
        );

        return "admin/admin";
    }

    //対応済みお問い合わせ履歴画面
    @GetMapping("/admin/contact/history")
    public String contactHistory(
        Model model,
        Principal principal
    ) {

        //対応済みお問い合わせ取得処理
        List<Contact> completedContacts =
            contactRepository.findByStatus(
                ContactStatus.COMPLETED
            );

        //対応完了日降順処理
        completedContacts.sort(
            Comparator.comparing(
                Contact::getCompletedAt,
                Comparator.nullsLast(
                    Comparator.naturalOrder()
                )
            ).reversed()
        );

        model.addAttribute(
            "completedContacts",
            completedContacts
        );

        model.addAttribute(
            "username",
            principal.getName()
        );

        return "admin/contactHistory";
    }

    //お問い合わせ対応内容・ステータス変更処理
    @PostMapping("/admin/contact/status/{id}")
    public String updateContactStatus(
        @PathVariable Long id,
        @RequestParam ContactStatus status,
        @RequestParam(required = false) String response,
        RedirectAttributes redirectAttributes
    ) {

        //お問い合わせ取得処理
        Contact contact =
            contactRepository
                .findById(id)
                .orElse(null);

        //存在しないお問い合わせの場合は管理画面へ戻す
        if (contact == null) {
            return "redirect:/admin";
        }

        //対応済み時の対応内容空白チェック
        if (
            status == ContactStatus.COMPLETED
            && (
                response == null
                || response.isBlank()
            )
        ) {

            redirectAttributes.addFlashAttribute(
                "contactResponseError",
                "対応済みにする場合は、回答・対応内容を入力してください。"
            );

            redirectAttributes.addFlashAttribute(
                "contactResponseErrorId",
                id
            );

            return "redirect:/admin";
        }

        //対応内容保存処理
        if (
            response != null
            && !response.isBlank()
        ) {

            contact.setResponse(
                response.trim()
            );
        }

        //対応ステータス変更処理
        contact.setStatus(
            status
        );

        //お問い合わせ保存処理
        contactRepository.save(
            contact
        );

        //対応済み以外の場合は同じモーダルを再表示
        if(status != ContactStatus.COMPLETED){
            redirectAttributes.addFlashAttribute("contactUpdatedId",id);
        }

        return "redirect:/admin";
    }

    //対応済みお問い合わせを通常一覧へ戻す処理
    @PostMapping("/admin/contact/history/status/{id}")
    public String restoreContactStatus(
        @PathVariable Long id,
        @RequestParam ContactStatus status
    ) {

        //お問い合わせ取得処理
        Contact contact =
            contactRepository
                .findById(id)
                .orElse(null);

        //存在しないお問い合わせの場合は履歴画面へ戻す
        if (contact == null) {
            return "redirect:/admin/contact/history";
        }

        //対応済み以外への変更のみ許可
        if (status == ContactStatus.COMPLETED) {
            return "redirect:/admin/contact/history";
        }

        //対応ステータス変更処理
        contact.setStatus(
            status
        );

        //お問い合わせ保存処理
        contactRepository.save(
            contact
        );

        return "redirect:/admin/contact/history";
    }

    //お問い合わせ返信処理
    @PostMapping("/admin/contact/reply/{id}")
    public String replyContact(
        @PathVariable Long id,
        @RequestParam String reply,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {

        //お問い合わせ取得処理
        Contact contact =
            contactRepository
                .findById(id)
                .orElse(null);

        //存在しないお問い合わせの場合は管理画面へ戻す
        if (contact == null) {
            return "redirect:/admin";
        }

        //返信内容空白チェック
        if (
            reply == null
            || reply.isBlank()
        ) {

            redirectAttributes.addFlashAttribute(
                "contactReplyError",
                "返信内容を入力してください"
            );

            redirectAttributes.addFlashAttribute(
                "contactReplyErrorId",
                id
            );

            return "redirect:/admin";
        }

        //返信済みチェック
        if (contact.getReply() != null) {

            redirectAttributes.addFlashAttribute(
                "contactReplyError",
                "このお問い合わせは既に返信しています"
            );

            redirectAttributes.addFlashAttribute(
                "contactReplyErrorId",
                id
            );

            return "redirect:/admin";
        }

        //ユーザーへの返信設定処理
        contact.reply(
            reply.trim(),
            principal.getName()
        );

        //お問い合わせ保存処理
        contactRepository.save(
            contact
        );

        //お問い合わせをしたユーザー取得処理
        User contactUser =
            userRepository
                .findByUsername(
                    contact.getUsername()
                )
                .orElse(null);

        //ユーザーとメールアドレスが存在する場合のみ返信通知メール送信
        if (
            contactUser != null
            && contactUser.getEmail() != null
            && !contactUser.getEmail().isBlank()
        ) {

            mailService.sendContactReplyNotificationMail(
                contactUser.getEmail(),
                contactUser.getUsername()
            );
        }

        redirectAttributes.addFlashAttribute(
            "contactReplySuccess",
            "ユーザーへ返信しました。"
        );

        return "redirect:/admin";
    }

    //ユーザー詳細画面処理
    @GetMapping("/admin/users/{id}")
    public String userDetail(
        @PathVariable Long id,
        Model model,
        Principal principal
    ) {

        //ユーザー取得処理
        User user =
            userRepository
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
    @Transactional
    @GetMapping("/admin/users/delete/{id}")
    public String deleteUser(
        @PathVariable Long id,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {

        //削除対象ユーザー取得処理
        User user =
            userRepository
                .findById(id)
                .orElse(null);

        //存在しないユーザーIDの場合は管理画面へ戻す
        if (user == null) {
            return "redirect:/admin";
        }

        //ログイン中の管理者自身は削除しない
        if (
            user.getUsername()
                .equals(
                    principal.getName()
                )
        ) {

            redirectAttributes.addFlashAttribute(
                "selfDeleteError",
                "管理者としてログインしているため、このアカウントは削除できません。"
            );

            return "redirect:/admin/users/" + id;
        }

        //ユーザーのタスク削除処理
        taskRepository.deleteByUser(
            user
        );

        //ユーザー削除処理
        userRepository.deleteById(
            id
        );

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
        @RequestParam String email,
        @RequestParam String password,
        @RequestParam String confirmPassword,
        Model model
    ) {

        //ユーザー名空白チェック
        if (
            username == null
            || username.isBlank()
        ) {

            model.addAttribute(
                "usernameError",
                "ユーザー名を入力してください"
            );

            return "admin/adminRegister";
        }

        username =
            username.trim();

        email =
            email.trim();

        model.addAttribute(
            "enteredUsername",
            username
        );

        model.addAttribute(
            "enteredEmail",
            email
        );

        //ユーザー名重複チェック
        if (
            userRepository
                .findByUsername(username)
                .isPresent()
        ) {

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
        if (
            !password.matches(
                ".*[A-Za-z].*"
            )
        ) {

            model.addAttribute(
                "passwordLetterError",
                "パスワードには英字を1文字以上含めてください"
            );

            return "admin/adminRegister";
        }

        //パスワード数字チェック
        if (
            !password.matches(
                ".*[0-9].*"
            )
        ) {

            model.addAttribute(
                "passwordNumberError",
                "パスワードには数字を1文字以上含めてください"
            );

            return "admin/adminRegister";
        }

        //パスワード一致チェック
        if (
            !password.equals(
                confirmPassword
            )
        ) {

            model.addAttribute(
                "passwordError",
                "パスワードが一致しません"
            );

            return "admin/adminRegister";
        }

        //管理者ユーザー登録処理
        User user =
            new User(
                username,
                email,
                passwordEncoder.encode(
                    password
                ),
                "ADMIN"
            );

        userRepository.save(
            user
        );

        //管理者向けお知らせ登録処理
        AdminNotification notification =
            new AdminNotification(
                user.getUsername()
                + "さんが管理者として登録されました。"
            );

        adminNotificationRepository.save(
            notification
        );

        return "redirect:/admin";
    }
}