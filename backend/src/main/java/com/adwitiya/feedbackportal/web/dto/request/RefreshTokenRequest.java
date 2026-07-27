package com.adwitiya.feedbackportal.web.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Exchanges a refresh token for a new access token. */
public record RefreshTokenRequest(

        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
