package com.adwitiya.feedbackportal.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static Optional<AppUserDetails> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof AppUserDetails details ? Optional.of(details) : Optional.empty();
    }

    public static Optional<Long> currentUserId() {
        return currentUser().map(AppUserDetails::id);
    }

    public static Optional<String> currentUserEmail() {
        return currentUser().map(AppUserDetails::email);
    }

    public static AppUserDetails requireCurrentUser() {
        return currentUser().orElseThrow(
                () -> new IllegalStateException("No authenticated principal in the security context"));
    }
}
