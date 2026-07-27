package com.adwitiya.feedbackportal.web.dto.response;

public record DepartmentResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean active
) {
}
