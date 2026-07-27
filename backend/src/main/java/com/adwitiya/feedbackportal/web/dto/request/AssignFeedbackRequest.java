package com.adwitiya.feedbackportal.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Assigns feedback to a specific administrator. */
public record AssignFeedbackRequest(

        @NotNull(message = "Assignee is required")
        @Positive
        Long adminUserId
) {
}
