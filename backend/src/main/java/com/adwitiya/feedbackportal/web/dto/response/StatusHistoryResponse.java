package com.adwitiya.feedbackportal.web.dto.response;

import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;

import java.time.Instant;

public record StatusHistoryResponse(
        FeedbackStatus fromStatus,
        FeedbackStatus toStatus,
        String changedByName,
        String note,
        Instant changedAt
) {
}
