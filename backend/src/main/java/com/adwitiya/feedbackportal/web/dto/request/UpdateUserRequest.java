package com.adwitiya.feedbackportal.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Profile fields a user or administrator may edit. Email and role are not editable here. */
public record UpdateUserRequest(

        @NotBlank @Size(max = 120)
        String fullName,

        @Pattern(regexp = "^$|^[+0-9 ()-]{7,20}$")
        String phone
) {
}
