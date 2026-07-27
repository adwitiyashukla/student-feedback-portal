package com.adwitiya.feedbackportal.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Creates a routing destination for feedback. */
public record CreateDepartmentRequest(

        @NotBlank
        @Pattern(regexp = "^[A-Z0-9_]{2,20}$", message = "Code must be upper-case letters, digits or underscores")
        String code,

        @NotBlank @Size(max = 120)
        String name,

        @Size(max = 500)
        String description
) {
}
