package com.adwitiya.feedbackportal.web.api;

import com.adwitiya.feedbackportal.security.AppUserDetails;
import com.adwitiya.feedbackportal.service.UserService;
import com.adwitiya.feedbackportal.web.dto.request.CreateAdminRequest;
import com.adwitiya.feedbackportal.web.dto.request.CreateStudentRequest;
import com.adwitiya.feedbackportal.web.dto.request.UpdateUserRequest;
import com.adwitiya.feedbackportal.web.dto.response.PageResponse;
import com.adwitiya.feedbackportal.web.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Account management. Staff only; there is no public sign-up. */
@Tag(name = "Users", description = "Student and administrator account management")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "List students")
    @GetMapping("/students")
    public PageResponse<UserResponse> listStudents(@RequestParam(required = false) Long departmentId,
                                                   @RequestParam(required = false) String search,
                                                   @PageableDefault(size = 20) Pageable pageable) {
        return userService.listStudents(departmentId, search, pageable);
    }

    @Operation(summary = "Create a student account")
    @PostMapping("/students")
    public ResponseEntity<UserResponse> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return ResponseEntity.status(201).body(userService.createStudent(request));
    }

    @Operation(summary = "List administrators")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/admins")
    public PageResponse<UserResponse> listAdmins(@PageableDefault(size = 20) Pageable pageable) {
        return userService.listAdmins(pageable);
    }

    @Operation(summary = "Create an administrator account")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/admins")
    public ResponseEntity<UserResponse> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.status(201).body(userService.createAdmin(request));
    }

    @Operation(summary = "Administrators in a department, for the assignment dropdown")
    @GetMapping("/admins/by-department/{departmentId}")
    public List<UserResponse> adminsInDepartment(@PathVariable Long departmentId) {
        return userService.adminsInDepartment(departmentId);
    }

    @Operation(summary = "Read one account")
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    @Operation(summary = "Update a profile")
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateProfile(id, request);
    }

    @Operation(summary = "Enable or disable an account",
            description = "Accounts are disabled, never deleted, so feedback history and the audit trail survive.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/enabled")
    public UserResponse setEnabled(@PathVariable Long id,
                                   @RequestParam boolean enabled,
                                   @AuthenticationPrincipal AppUserDetails principal) {
        return userService.setEnabled(id, enabled, principal.id());
    }

    @Operation(summary = "Clear a lockout early")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/unlock")
    public UserResponse unlock(@PathVariable Long id) {
        return userService.unlock(id);
    }
}
