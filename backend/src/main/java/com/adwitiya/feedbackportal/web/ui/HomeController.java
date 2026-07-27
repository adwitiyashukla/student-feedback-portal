package com.adwitiya.feedbackportal.web.ui;

import com.adwitiya.feedbackportal.security.AppUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Public pages: landing, sign-in and the access-denied page. */
@Controller
public class HomeController {

    /** Sends an already-signed-in visitor to their own dashboard. */
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

    /**
     * Access-denied page. Mapped for every HTTP method, not just GET: Spring
     * Security <em>forwards</em> to this path, preserving the original method,
     * so a rejected POST (an expired CSRF token on the sign-in form, for
     * instance) arrives here as a POST. A GET-only mapping turned that into a
     * 405 and then a 500 instead of showing the page.
     */
    @RequestMapping("/error/403")
    public String forbidden() {
        return "error/403";
    }
}
