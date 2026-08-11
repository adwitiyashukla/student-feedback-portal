package com.adwitiya.feedbackportal.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCommentRequest(

        @NotBlank(message = "Comment body is required")
        @Size(min = 2, max = 4000, message = "Comment must be between 2 and 4000 characters")
        String body,

        @Schema(description = "Staff-only note, hidden from the student", defaultValue = "false")
        boolean internalNote
) {
}
