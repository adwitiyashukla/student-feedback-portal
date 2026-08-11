package com.adwitiya.feedbackportal.web.api;

import com.adwitiya.feedbackportal.security.AppUserDetails;
import com.adwitiya.feedbackportal.service.NotificationService;
import com.adwitiya.feedbackportal.web.dto.response.NotificationResponse;
import com.adwitiya.feedbackportal.web.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Notifications", description = "In-app notification inbox")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @Operation(summary = "List the caller's notifications")
    @GetMapping
    public PageResponse<NotificationResponse> list(@AuthenticationPrincipal AppUserDetails principal,
                                                   @PageableDefault(size = 20) Pageable pageable) {
        return notificationService.list(principal.id(), pageable);
    }

    @Operation(summary = "Unread notification count, for the header badge")
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AppUserDetails principal) {
        return Map.of("unread", notificationService.unreadCount(principal.id()));
    }

    @Operation(summary = "Mark every notification as read")
    @PostMapping("/mark-all-read")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal AppUserDetails principal) {
        return Map.of("updated", notificationService.markAllRead(principal.id()));
    }
}
