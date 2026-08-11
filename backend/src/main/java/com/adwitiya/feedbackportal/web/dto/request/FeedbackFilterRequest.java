package com.adwitiya.feedbackportal.web.dto.request;

import com.adwitiya.feedbackportal.domain.enums.FeedbackCategory;
import com.adwitiya.feedbackportal.domain.enums.FeedbackPriority;
import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

@Schema(description = "Optional filters for listing feedback")
public record FeedbackFilterRequest(

        FeedbackStatus status,

        FeedbackCategory category,

        FeedbackPriority priority,

        Long departmentId,

        Long assignedToId,

        @Schema(description = "Free-text match on ticket number, title or description")
        String search,

        @Schema(description = "Only tickets past their SLA deadline")
        Boolean overdue,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant from,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant to
) {
    public FeedbackFilterRequest {
        overdue = overdue != null && overdue;
    }
}
