package com.adwitiya.feedbackportal.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Self-service password change.
 *
 * <p>The current password is required even though the caller is already
 * authenticated: it stops a hijacked session from locking the real owner out.
 * All refresh tokens are revoked on success.</p>
 */
public record ChangePasswordRequest(

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 10, max = 128, message = "Password must be at least 10 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must contain an upper-case letter, a lower-case letter and a digit")
        String newPassword
) {
}
