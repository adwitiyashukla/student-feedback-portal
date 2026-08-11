package com.adwitiya.feedbackportal.web.dto.response;

import com.adwitiya.feedbackportal.domain.enums.Role;

import java.time.Instant;

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
