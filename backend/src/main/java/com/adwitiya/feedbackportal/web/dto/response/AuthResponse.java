package com.adwitiya.feedbackportal.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Issued token pair")
public record AuthResponse(

        @Schema(description = "Short-lived bearer token")
        String accessToken,

        @Schema(description = "Opaque token used to obtain the next access token")
        String refreshToken,

        @Schema(defaultValue = "Bearer")
        String tokenType,

        @Schema(description = "Access token lifetime in seconds", example = "900")
        long expiresIn,

        UserResponse user
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
