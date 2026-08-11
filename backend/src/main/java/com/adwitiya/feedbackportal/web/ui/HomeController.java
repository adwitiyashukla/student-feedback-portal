package com.adwitiya.feedbackportal.web.ui;

import com.adwitiya.feedbackportal.security.AppUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String index(@AuthenticationPrincipal AppUserDetails principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        return principal.isStaff() ? "redirect:/admin/dashboard" : "redirect:/student/dashboard";
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal AppUserDetails principal) {
        if (principal != null) {
            return "redirect:/";
        }
        return "auth/login";
    }

    @RequestMapping("/error/403")
    public String forbidden() {
        return "error/403";
    }
}
