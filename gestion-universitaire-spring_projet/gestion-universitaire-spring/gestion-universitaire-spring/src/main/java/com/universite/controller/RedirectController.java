package com.universite.controller;

import com.universite.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class RedirectController {

    @GetMapping("/redirect-by-role")
    public String redirectByRole(@AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/login";
        }

        return switch (user.getRole().getName()) {
            case ROLE_ADMIN -> "redirect:/admin/dashboard";
            case ROLE_ENSEIGNANT -> "redirect:/enseignant/dashboard";
            case ROLE_ETUDIANT -> "redirect:/etudiant/dashboard";
            default -> "redirect:/login";
        };
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
}