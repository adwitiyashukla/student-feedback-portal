package com.adwitiya.feedbackportal.web.api;

import com.adwitiya.feedbackportal.security.AppUserDetails;
import com.adwitiya.feedbackportal.service.AuthService;
import com.adwitiya.feedbackportal.web.dto.request.ChangePasswordRequest;
import com.adwitiya.feedbackportal.web.dto.request.LoginRequest;
import com.adwitiya.feedbackportal.web.dto.request.RefreshTokenRequest;
import com.adwitiya.feedbackportal.web.dto.response.AuthResponse;
import com.adwitiya.feedbackportal.web.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authentication endpoints. */
@Tag(name = "Authentication", description = "Sign in, refresh, sign out and change password")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Sign in and receive an access/refresh token pair",
            security = @SecurityRequirement(name = ""))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "423", description = "Account locked", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "429", description = "Too many attempts", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    @Operation(summary = "Exchange a refresh token for a new token pair",
            description = "The presented refresh token is revoked; reuse of a revoked token drops all sessions.",
            security = @SecurityRequirement(name = ""))
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken(), httpRequest));
    }

    @Operation(summary = "Revoke the supplied refresh token", security = @SecurityRequirement(name = ""))
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(request != null ? request.refreshToken() : null);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Revoke every session belonging to the caller")
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutEverywhere(@AuthenticationPrincipal AppUserDetails principal) {
        authService.logoutEverywhere(principal.id());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Profile of the currently authenticated user")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> currentUser(@AuthenticationPrincipal AppUserDetails principal) {
        return ResponseEntity.ok(authService.currentUserProfile(principal.id()));
    }

    @Operation(summary = "Change the caller's own password",
            description = "Requires the current password and revokes all active sessions on success.")
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AppUserDetails principal,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.id(), request);
        return ResponseEntity.noContent().build();
    }
}
