package com.adwitiya.feedbackportal.web.dto.response;

import java.time.Instant;

public record AttachmentResponse(
        Long id,
        String fileName,
        String contentType,
        String size,
        String downloadUrl,
        Instant createdAt
) {
}
