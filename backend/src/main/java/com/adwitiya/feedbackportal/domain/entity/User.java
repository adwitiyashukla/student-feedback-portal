package com.adwitiya.feedbackportal.domain.entity;

import com.adwitiya.feedbackportal.domain.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * A single identity for everyone who can sign in.
 *
 * <p>{@code passwordHash} holds a BCrypt digest. The column is 100 characters
 * because a BCrypt hash is 60 and the extra room lets the encoder be upgraded
 * (Spring Security's {@code DelegatingPasswordEncoder} prefixes the algorithm)
 * without another migration.</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    /** Consecutive failed sign-ins before the account is temporarily locked. */
    public static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Builder.Default
    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked = false;

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "lock_expires_at")
    private Instant lockExpiresAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Version
    @Builder.Default
    private Long version = 0L;

    /**
     * Records a failed sign-in, locking the account once the threshold is hit.
     *
     * @param lockDurationSeconds how long the resulting lock should last
     */
    public void registerFailedLogin(long lockDurationSeconds) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            this.accountLocked = true;
            this.lockExpiresAt = Instant.now().plusSeconds(lockDurationSeconds);
        }
    }

    /** Clears the failure counter and stamps the sign-in time. */
    public void registerSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.accountLocked = false;
        this.lockExpiresAt = null;
        this.lastLoginAt = Instant.now();
    }

    /**
     * @return {@code true} when the account is locked and the lock has not yet expired
     */
    public boolean isCurrentlyLocked() {
        if (!accountLocked) {
            return false;
        }
        return lockExpiresAt == null || lockExpiresAt.isAfter(Instant.now());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User user)) {
            return false;
        }
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", email='" + email + "', role=" + role + "}";
    }
}
