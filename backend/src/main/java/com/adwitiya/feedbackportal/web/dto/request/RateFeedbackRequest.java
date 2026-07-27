package com.adwitiya.feedbackportal.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Student satisfaction rating, accepted only once a ticket is resolved. */
public record RateFeedbackRequest(

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        Integer rating
) {
}
