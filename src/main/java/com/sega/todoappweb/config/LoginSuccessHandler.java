package com.sega.todoappweb.config;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.sega.todoappweb.contact.ContactRepository;
import com.sega.todoappweb.user.User;
import com.sega.todoappweb.user.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final ContactRepository contactRepository;

    public LoginSuccessHandler(
        UserRepository userRepository,
        ContactRepository contactRepository
    ) {
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
    }

    //ログイン成功後処理
    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {

        //ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(
                    authentication.getName()
                )
                .orElseThrow();

        //管理者ログイン処理
        if ("ADMIN".equals(loginUser.getRole())) {

            LocalDateTime lastLoginAt =
                loginUser.getLastLoginAt();

            boolean hasNewContact;

            //初回ログイン日時未登録の場合
            if (lastLoginAt == null) {

                hasNewContact =
                    contactRepository.count() > 0;

            } else {

                //前回ログイン後のお問い合わせ確認処理
                hasNewContact =
                    !contactRepository
                        .findByCreatedAtAfter(
                            lastLoginAt
                        )
                        .isEmpty();
            }

            //今回ログイン日時更新処理
            loginUser.setLastLoginAt(
                LocalDateTime.now()
            );

            userRepository.save(
                loginUser
            );

            //新しいお問い合わせがある場合
            if (hasNewContact) {

                response.sendRedirect(
                    "/admin?newContact=true"
                );

                return;
            }

            response.sendRedirect(
                "/admin"
            );

            return;
        }

        //一般ユーザー未読返信件数取得処理
        long unreadReplyCount =
            contactRepository
                .countByUsernameAndReplyReadFalseAndReplyIsNotNull(
                    loginUser.getUsername()
                );

        //未読返信がある場合
        if (unreadReplyCount > 0) {

            response.sendRedirect(
                "/?newReply=true"
            );

            return;
        }

        //一般ユーザーログイン処理
        response.sendRedirect(
            "/"
        );
    }
}