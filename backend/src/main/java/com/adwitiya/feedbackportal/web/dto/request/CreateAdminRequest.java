package com.adwitiya.feedbackportal.web.dto.request;

import com.adwitiya.feedbackportal.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAdminRequest(

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
        @Pattern(regexp = "^[A-Za-z0-9-]{3,30}$")
        String employeeCode,

        @NotNull
        Long departmentId,

        @Size(max = 80)
        String designation,

        @NotNull(message = "Role is required")
        Role role,

        @Pattern(regexp = "^$|^[+0-9 ()-]{7,20}$")
        String phone
) {
}
