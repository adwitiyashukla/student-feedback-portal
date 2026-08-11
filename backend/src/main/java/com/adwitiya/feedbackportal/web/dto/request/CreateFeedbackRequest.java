package com.adwitiya.feedbackportal.web.dto.request;

import com.adwitiya.feedbackportal.domain.enums.FeedbackCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "New feedback submission")
public record CreateFeedbackRequest(

        @Schema(example = "Lab computers extremely slow in Block C")
        @NotBlank(message = "Title is required")
        @Size(min = 10, max = 150, message = "Title must be between 10 and 150 characters")
        String title,

        @Schema(example = "The machines in the Block C programming lab take almost ten minutes to boot...")
        @NotBlank(message = "Description is required")
        @Size(min = 20, max = 5000, message = "Description must be between 20 and 5000 characters")
        String description,

        @NotNull(message = "Category is required")
        FeedbackCategory category,

        @NotNull(message = "Department is required")
        @Positive(message = "Department id must be positive")
        Long departmentId,

        @Schema(description = "Hide the submitter's identity from department staff", defaultValue = "false")
        Boolean anonymous
) {
    public CreateFeedbackRequest {
        anonymous = anonymous != null && anonymous;
    }
}
