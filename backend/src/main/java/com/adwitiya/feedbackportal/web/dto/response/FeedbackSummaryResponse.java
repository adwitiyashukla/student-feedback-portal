package com.adwitiya.feedbackportal.web.dto.response;

import com.adwitiya.feedbackportal.domain.enums.FeedbackCategory;
import com.adwitiya.feedbackportal.domain.enums.FeedbackPriority;
import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
import com.adwitiya.feedbackportal.domain.enums.SentimentLabel;

import java.time.Instant;

public record FeedbackSummaryResponse(
        Long id,
        String ticketNumber,
        String title,
        FeedbackCategory category,
        FeedbackPriority priority,
        FeedbackStatus status,
        String departmentName,
        String submittedByName,
        String assignedToName,
        SentimentLabel sentimentLabel,
        boolean overdue,
        Instant dueAt,
        Instant createdAt
) {
}
