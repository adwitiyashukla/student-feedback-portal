package com.adwitiya.feedbackportal.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * JWT signing and lifetime configuration, bound from {@code app.jwt.*}.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Minimum key length HS512 requires, in bytes. */
    public static final int MIN_SECRET_BYTES = 64;

    /** Base64-encoded HMAC signing key. Supply via {@code JWT_SECRET}. */
    @NotBlank
    private String secret;

    /**
     * Lifetime of an access token. Short by design; refresh tokens do the rest.
     *
     * <p>Bean Validation has no built-in constraint for {@link Duration} —
     * {@code @Positive} only understands numeric types — so the durations are
     * checked together in {@link #isDurationsPositive()}.</p>
     */
    private Duration accessTokenTtl = Duration.ofMinutes(15);

    /** Lifetime of a refresh token. */
    private Duration refreshTokenTtl = Duration.ofDays(7);

    /** {@code iss} claim written into every issued token. */
    @NotBlank
    private String issuer = "student-feedback-portal";

    /** How long an account stays locked after too many failed sign-ins. */
    private Duration lockoutDuration = Duration.ofMinutes(15);

    /**
     * Rejects zero or negative lifetimes at startup.
     *
     * @return {@code true} when every configured duration is positive
     */
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
