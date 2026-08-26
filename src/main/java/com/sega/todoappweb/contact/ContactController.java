package com.sega.todoappweb.contact;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sega.todoappweb.admin.AdminNotification;
import com.sega.todoappweb.admin.AdminNotificationRepository;
import com.sega.todoappweb.admin.AdminNotificationType;
import com.sega.todoappweb.user.User;
import com.sega.todoappweb.user.UserRepository;
import com.sega.todoappweb.mail.MailService;

@Controller
public class ContactController {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final AdminNotificationRepository adminNotificationRepository;
    private final MailService mailService;

    public ContactController(
        ContactRepository contactRepository,
        UserRepository userRepository,
        AdminNotificationRepository adminNotificationRepository,
        MailService mailService
    ) {
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
        this.adminNotificationRepository = adminNotificationRepository;
        this.mailService = mailService;
    }

    //お問い合わせ送信処理
    @PostMapping("/contact")
    public String contact(
        @RequestParam ContactType contactType,
        @RequestParam String content,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {

        //ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(
                    principal.getName()
                )
                .orElseThrow();

        //お問い合わせ内容空白チェック
        if (
            content == null
            || content.isBlank()
        ) {

            redirectAttributes.addFlashAttribute(
                "contactError",
                "お問い合わせ内容を入力してください。"
            );

            return "redirect:/";
        }

        //お問い合わせ作成処理
        Contact contact =
            new Contact(
                loginUser.getUsername(),
                contactType,
                content.trim()
            );

        //お問い合わせ保存処理
        contactRepository.save(
            contact
        );

        //管理者向けお問い合わせ通知作成処理
        AdminNotification notification =
            new AdminNotification(
                loginUser.getUsername()
                + "さんから新しいお問い合わせが届きました。",
                AdminNotificationType.CONTACT
            );

        //管理者向けお問い合わせ通知保存処理
        adminNotificationRepository.save(
            notification
        );

        //管理者へお問い合わせ通知メール送信処理
        mailService.sendContactNotificationMail(
            loginUser.getUsername(), 
            contactType.getDisplayName()
        );
        

        //お問い合わせ送信完了メッセージ設定処理
        redirectAttributes.addFlashAttribute(
            "contactSuccess",
            "お問い合わせを送信しました。"
        );

        return "redirect:/";
    }

    //お問い合わせ履歴画面
    @GetMapping("/contact/history")
    public String contactHistory(
        Model model,
        Principal principal
    ) {

        //ログインユーザーのお問い合わせ履歴取得処理
        List<Contact> contacts =
            contactRepository
                .findByUsernameOrderByCreatedAtDesc(
                    principal.getName()
                );

        model.addAttribute(
            "contacts",
            contacts
        );

        model.addAttribute(
            "username",
            principal.getName()
        );

        return "contact/contactHistory";
    }

    //お問い合わせ返信確認済み処理
    @PostMapping("/contact/history/read/{id}")
    public String readReply(
        @PathVariable Long id,
        Principal principal
    ) {

        //ユーザー本人のお問い合わせ取得処理
        Contact contact =
            contactRepository
                .findByIdAndUsername(
                    id,
                    principal.getName()
                )
                .orElse(null);

        //存在しないお問い合わせの場合は履歴画面へ戻す
        if (contact == null) {
            return "redirect:/contact/history";
        }

        //返信がある場合のみ確認済みにする
        if (contact.getReply() != null) {

            contact.readReply();

            contactRepository.save(
                contact
            );
        }

        return "redirect:/contact/history";
    }
}