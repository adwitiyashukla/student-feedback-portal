package com.adwitiya.feedbackportal.service;

import com.adwitiya.feedbackportal.config.properties.JwtProperties;
import com.adwitiya.feedbackportal.domain.entity.RefreshToken;
import com.adwitiya.feedbackportal.domain.entity.User;
import com.adwitiya.feedbackportal.exception.BusinessRuleException;
import com.adwitiya.feedbackportal.exception.ResourceNotFoundException;
import com.adwitiya.feedbackportal.repository.AdminRepository;
import com.adwitiya.feedbackportal.repository.RefreshTokenRepository;
import com.adwitiya.feedbackportal.repository.StudentRepository;
import com.adwitiya.feedbackportal.repository.UserRepository;
import com.adwitiya.feedbackportal.security.AppUserDetails;
import com.adwitiya.feedbackportal.security.JwtService;
import com.adwitiya.feedbackportal.web.dto.request.ChangePasswordRequest;
import com.adwitiya.feedbackportal.web.dto.request.LoginRequest;
import com.adwitiya.feedbackportal.web.dto.response.AuthResponse;
import com.adwitiya.feedbackportal.web.dto.response.UserResponse;
import com.adwitiya.feedbackportal.web.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuditService auditService;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.email().trim().toLowerCase();
        String ip = clientIp(httpRequest);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), "$2a$12$" + "x".repeat(53));
            auditService.recordAuthEvent("LOGIN_FAILED", email, ip, "No such account");
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.isCurrentlyLocked()) {
            auditService.recordAuthEvent("LOGIN_BLOCKED", email, ip, "Account locked");
            throw new LockedException("Account is temporarily locked");
        }
        if (!user.isEnabled()) {
            auditService.recordAuthEvent("LOGIN_BLOCKED", email, ip, "Account disabled");
            throw new DisabledException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.registerFailedLogin(jwtProperties.getLockoutDuration().toSeconds());
            userRepository.save(user);
            auditService.recordAuthEvent("LOGIN_FAILED", email, ip,
                    "Wrong password (attempt %d)".formatted(user.getFailedLoginAttempts()));
            throw new BadCredentialsException("Invalid email or password");
        }

        user.registerSuccessfulLogin();
        userRepository.save(user);
        auditService.recordAuthEvent("LOGIN_SUCCESS", email, ip, null);

        AppUserDetails principal = toPrincipal(user);
        return issueTokens(user, principal, httpRequest);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken, HttpServletRequest httpRequest) {
        String hash = jwtService.hashRefreshToken(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!stored.isUsable()) {
            refreshTokenRepository.revokeAllForUser(stored.getUser().getId());
            auditService.recordAuthEvent("REFRESH_REUSE_DETECTED", stored.getUser().getEmail(),
                    clientIp(httpRequest), "All sessions revoked");
            throw new BadCredentialsException("Refresh token is no longer valid");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        if (!user.isEnabled() || user.isCurrentlyLocked()) {
            throw new DisabledException("Account is no longer active");
        }
        return issueTokens(user, toPrincipal(user), httpRequest);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(jwtService.hashRefreshToken(rawRefreshToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void logoutEverywhere(Long userId) {
        int revoked = refreshTokenRepository.revokeAllForUser(userId);
        log.info("Revoked {} refresh tokens for user {}", revoked, userId);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            auditService.record("PASSWORD_CHANGE_FAILED", "User", userId, "Wrong current password");
            throw new BusinessRuleException("The current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("The new password must differ from the current one");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(userId);
        auditService.record("PASSWORD_CHANGED", "User", userId, "All sessions revoked");
    }

    @Transactional(readOnly = true)
    public UserResponse currentUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        if (user.getRole().isStaff()) {
            return adminRepository.findByUserId(userId)
                    .map(userMapper::toResponse)
                    .orElseGet(() -> userMapper.toResponse(user));
        }
        return studentRepository.findByUserId(userId)
                .map(userMapper::toResponse)
                .orElseGet(() -> userMapper.toResponse(user));
    }

    private AuthResponse issueTokens(User user, AppUserDetails principal, HttpServletRequest httpRequest) {
        String accessToken = jwtService.generateAccessToken(principal);
        String rawRefreshToken = jwtService.generateRefreshTokenValue();

        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(jwtService.hashRefreshToken(rawRefreshToken))
                .user(user)
                .issuedAt(Instant.now())
                .expiresAt(jwtService.refreshTokenExpiry())
                .revoked(false)
                .userAgent(truncate(header(httpRequest, "User-Agent"), 255))
                .ipAddress(clientIp(httpRequest))
                .build());

        return AuthResponse.of(accessToken, rawRefreshToken,
                jwtService.accessTokenTtlSeconds(), currentUserProfile(user.getId()));
    }

    private AppUserDetails toPrincipal(User user) {
        Long departmentId = user.getRole().isStaff()
                ? adminRepository.findByUserId(user.getId())
                        .map(admin -> admin.getDepartment().getId())
                        .orElse(null)
                : null;
        return AppUserDetails.from(user, departmentId);
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return truncate(forwarded.split(",")[0].trim(), 45);
        }
        return truncate(request.getRemoteAddr(), 45);
    }

    private String header(HttpServletRequest request, String name) {
        return request == null ? null : request.getHeader(name);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
