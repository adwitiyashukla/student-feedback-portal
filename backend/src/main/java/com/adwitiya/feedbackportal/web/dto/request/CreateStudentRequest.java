package com.adwitiya.feedbackportal.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Creates a student account. Administrator-only; there is no public sign-up. */
public record CreateStudentRequest(

        @NotBlank @Email @Size(max = 160)
        String email,

        @NotBlank
        @Size(min = 10, max = 128, message = "Password must be at least 10 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must contain an upper-case letter, a lower-case letter and a digit")
        String password,

        @NotBlank @Size(max = 120)
        String fullName,

        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9-]{4,30}$", message = "Roll number may contain letters, digits and hyphens only")
        String rollNumber,

        @NotNull
        Long departmentId,

        @Size(max = 60)
        String program,

        @Min(1990) @Max(2100)
        Integer batchYear,

        @Min(1) @Max(12)
        Integer semester,

        @Pattern(regexp = "^$|^[+0-9 ()-]{7,20}$", message = "Phone number format is invalid")
        String phone
) {
}
