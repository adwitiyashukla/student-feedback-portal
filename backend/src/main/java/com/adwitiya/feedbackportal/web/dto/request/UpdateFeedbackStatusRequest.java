package com.adwitiya.feedbackportal.web.dto.request;

import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateFeedbackStatusRequest(

        @NotNull(message = "Target status is required")
        FeedbackStatus status,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
}
