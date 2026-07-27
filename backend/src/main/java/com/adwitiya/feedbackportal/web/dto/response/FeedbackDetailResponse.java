package com.adwitiya.feedbackportal.web.dto.response;

import com.adwitiya.feedbackportal.domain.enums.FeedbackCategory;
import com.adwitiya.feedbackportal.domain.enums.FeedbackPriority;
import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
import com.adwitiya.feedbackportal.domain.enums.SentimentLabel;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Full view of one piece of feedback.
 *
 * <p>{@code allowedTransitions} is computed server-side so the UI renders only
 * the buttons the state machine will actually accept, and the API stays the
 * single source of truth for what is legal.</p>
 */
public record FeedbackDetailResponse(
        Long id,
        String ticketNumber,
        String title,
        String description,
        FeedbackCategory category,
        FeedbackPriority priority,
        FeedbackStatus status,
        Set<FeedbackStatus> allowedTransitions,
        String departmentName,
        Long departmentId,
        String submittedByName,
        String submittedByRoll,
        boolean anonymous,
        String assignedToName,
        Long assignedToId,
        SentimentLabel sentimentLabel,
        Double sentimentScore,
        FeedbackCategory suggestedCategory,
        FeedbackPriority suggestedPriority,
        Double analysisConfidence,
        Integer satisfactionRating,
        boolean overdue,
        Instant dueAt,
        Instant resolvedAt,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        List<CommentResponse> comments,
        List<StatusHistoryResponse> history,
        List<AttachmentResponse> attachments
) {
}
