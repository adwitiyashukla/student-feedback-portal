package com.adwitiya.feedbackportal.web.dto.response;

import com.adwitiya.feedbackportal.domain.enums.Role;

import java.time.Instant;

public record CommentResponse(
        Long id,
        String authorName,
        Role authorRole,
        String body,
        boolean internalNote,
        Instant createdAt
) {
}
