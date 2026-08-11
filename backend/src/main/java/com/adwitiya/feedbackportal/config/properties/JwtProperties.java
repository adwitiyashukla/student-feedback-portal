package com.adwitiya.feedbackportal.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    public static final int MIN_SECRET_BYTES = 64;

    @NotBlank
    private String secret;

    private Duration accessTokenTtl = Duration.ofMinutes(15);

    private Duration refreshTokenTtl = Duration.ofDays(7);

    @NotBlank
    private String issuer = "student-feedback-portal";

    private Duration lockoutDuration = Duration.ofMinutes(15);

    @AssertTrue(message = "app.jwt token lifetimes and lockout duration must all be positive")
    public boolean isDurationsPositive() {
        return isPositive(accessTokenTtl)
                && isPositive(refreshTokenTtl)
                && isPositive(lockoutDuration);
    }

    private boolean isPositive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }
}
