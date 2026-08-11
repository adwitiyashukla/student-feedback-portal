package com.adwitiya.feedbackportal.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignFeedbackRequest(

        @NotNull(message = "Assignee is required")
        @Positive
        Long adminUserId
) {
}
