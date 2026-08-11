package com.adwitiya.feedbackportal.unit;

import com.adwitiya.feedbackportal.domain.entity.User;
import com.adwitiya.feedbackportal.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User account lockout")
class UserLockoutTest {
    private static final long LOCK_SECONDS = 900;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("victim@university.edu")
                .passwordHash("{bcrypt}$2a$12$abcdefghijklmnopqrstuv")
                .fullName("Test User")
                .role(Role.STUDENT)
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build();
    }

    @Test
    void staysUnlockedBelowTheThreshold() {
        for (int attempt = 1; attempt < User.MAX_FAILED_LOGIN_ATTEMPTS; attempt++) {
            user.registerFailedLogin(LOCK_SECONDS);
        }

        assertThat(user.getFailedLoginAttempts()).isEqualTo(User.MAX_FAILED_LOGIN_ATTEMPTS - 1);
        assertThat(user.isCurrentlyLocked()).isFalse();
    }

    @Test
    void locksExactlyAtTheThreshold() {
        for (int attempt = 0; attempt < User.MAX_FAILED_LOGIN_ATTEMPTS; attempt++) {
            user.registerFailedLogin(LOCK_SECONDS);
        }

        assertThat(user.isAccountLocked()).isTrue();
        assertThat(user.isCurrentlyLocked()).isTrue();
        assertThat(user.getLockExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void lockLapsesOnceTheWindowPasses() {
        for (int attempt = 0; attempt < User.MAX_FAILED_LOGIN_ATTEMPTS; attempt++) {
            user.registerFailedLogin(LOCK_SECONDS);
        }
        user.setLockExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));

        assertThat(user.isAccountLocked()).isTrue();
        assertThat(user.isCurrentlyLocked()).isFalse();
    }

    @Test
    void successfulSignInClearsEverything() {
        user.registerFailedLogin(LOCK_SECONDS);
        user.registerFailedLogin(LOCK_SECONDS);

        user.registerSuccessfulLogin();

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.isAccountLocked()).isFalse();
        assertThat(user.getLockExpiresAt()).isNull();
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    void successfulSignInReleasesAnActiveLock() {
        for (int attempt = 0; attempt < User.MAX_FAILED_LOGIN_ATTEMPTS; attempt++) {
            user.registerFailedLogin(LOCK_SECONDS);
        }
        assertThat(user.isCurrentlyLocked()).isTrue();

        user.registerSuccessfulLogin();

        assertThat(user.isCurrentlyLocked()).isFalse();
    }
}
