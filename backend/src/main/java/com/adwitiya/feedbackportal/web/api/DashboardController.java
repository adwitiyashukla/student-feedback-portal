package com.adwitiya.feedbackportal.web.api;

import com.adwitiya.feedbackportal.security.AppUserDetails;
import com.adwitiya.feedbackportal.service.DashboardService;
import com.adwitiya.feedbackportal.service.FeedbackService;
import com.adwitiya.feedbackportal.web.dto.response.DashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Dashboard", description = "Aggregated feedback statistics")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;
    private final FeedbackService feedbackService;

    @Operation(summary = "Institution or department statistics")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping("/admin")
    public DashboardResponse adminDashboard(@RequestParam(required = false) Long departmentId,
                                            @AuthenticationPrincipal AppUserDetails principal) {
        Long scope = principal.isSuperAdmin() ? departmentId : principal.departmentId();
        return dashboardService.build(scope);
    }

    @Operation(summary = "Monthly submitted-versus-resolved trend")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping("/admin/trend")
    public List<DashboardResponse.TrendPoint> trend(@RequestParam(required = false) Long departmentId,
                                                    @AuthenticationPrincipal AppUserDetails principal) {
        Long scope = principal.isSuperAdmin() ? departmentId : principal.departmentId();
        return dashboardService.monthlyTrend(scope);
    }

    @Operation(summary = "Counters for the signed-in student")
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/student")
    public Map<String, Long> studentDashboard(@AuthenticationPrincipal AppUserDetails principal) {
        return Map.of("activeFeedback", feedbackService.countActiveForStudent(principal.id()));
    }
}
