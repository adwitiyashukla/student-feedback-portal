package com.adwitiya.feedbackportal.web.dto.response;

import com.adwitiya.feedbackportal.domain.enums.Role;

import java.time.Instant;

/** Public view of an account. The password hash is never part of any response. */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        Role role,
        boolean enabled,
        boolean locked,
        String departmentCode,
        String departmentName,
        String rollNumber,
        String employeeCode,
        Instant lastLoginAt,
        Instant createdAt
) {
}
