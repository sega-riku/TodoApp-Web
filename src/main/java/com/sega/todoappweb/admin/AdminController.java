package com.sega.todoappweb.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.security.Principal;

import com.sega.todoappweb.user.User;
import com.sega.todoappweb.user.UserRepository;


@Controller
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    //管理者画面
    @GetMapping("/admin")
    public String admin(Model model, Principal principal) {

        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);

        model.addAttribute("username", principal.getName());

        long totalUsers = users.size();

        //管理者数カウント処理
        long adminCount = users.stream()
                          .filter(user -> "ADMIN".equals(user.getRole()))
                          .count();

        //ユーザー数カウント処理
        long userCount = users.stream()
                         .filter(user -> "USER".equals(user.getRole()))
                         .count();

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("userCount", userCount);

        return "admin/admin";
    }

    //ユーザー詳細画面処理
    @GetMapping("/admin/users/{id}")
    public String userDetail(
        @PathVariable Long id,
        Model model,
        Principal principal
    ) {

        User user = userRepository
            .findById(id)
            .orElseThrow();

        model.addAttribute("user", user);
        model.addAttribute("username", principal.getName());

        return "admin/adminUserDetail";
    }

    //ユーザー削除処理
    @GetMapping("/admin/users/delete/{id}")
    public String deleteUser(
        @PathVariable Long id,
        Principal principal
    ) {

        User user = userRepository
            .findById(id)
            .orElseThrow();

        if (user.getUsername().equals(principal.getName())) {
            return "redirect:/admin";
        }

        userRepository.deleteById(id);

        return "redirect:/admin";
    }
}