package com.adwitiya.feedbackportal.web.ui;

import com.adwitiya.feedbackportal.domain.enums.FeedbackCategory;
import com.adwitiya.feedbackportal.domain.enums.FeedbackPriority;
import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
import com.adwitiya.feedbackportal.security.AppUserDetails;
import com.adwitiya.feedbackportal.service.DashboardService;
import com.adwitiya.feedbackportal.service.DepartmentService;
import com.adwitiya.feedbackportal.service.FeedbackService;
import com.adwitiya.feedbackportal.service.NotificationService;
import com.adwitiya.feedbackportal.service.UserService;
import com.adwitiya.feedbackportal.web.dto.request.FeedbackFilterRequest;
import com.adwitiya.feedbackportal.web.dto.response.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

/** Server-rendered staff area. */
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminUiController {

    private final DashboardService dashboardService;
    private final FeedbackService feedbackService;
    private final DepartmentService departmentService;
    private final UserService userService;
    private final NotificationService notificationService;

    @GetMapping("/admin/dashboard")
    public String dashboard(@AuthenticationPrincipal AppUserDetails principal,
                            @RequestParam(required = false) Long departmentId,
                            Model model) {
        // A department administrator is pinned to their own department.
        Long scope = principal.isSuperAdmin() ? departmentId : principal.departmentId();
        DashboardResponse stats = dashboardService.build(scope);

        model.addAttribute("stats", stats);
        model.addAttribute("departments", departmentService.listActive());
        model.addAttribute("selectedDepartmentId", scope);
        model.addAttribute("assignedCount", feedbackService.countAssignedActive(principal.id()));
        model.addAttribute("notifications", notificationService.recent(principal.id()));
        model.addAttribute("unreadCount", notificationService.unreadCount(principal.id()));
        return "admin/dashboard";
    }

    @GetMapping("/admin/feedback")
    public String feedbackQueue(@AuthenticationPrincipal AppUserDetails principal,
                                @ModelAttribute FeedbackFilterRequest filter,
                                @PageableDefault(size = 20, sort = "createdAt",
                                        direction = Sort.Direction.DESC) Pageable pageable,
                                Model model) {
        model.addAttribute("page", feedbackService.list(filter, principal, pageable));
        model.addAttribute("filter", filter);
        model.addAttribute("statuses", FeedbackStatus.values());
        model.addAttribute("categories", FeedbackCategory.values());
        model.addAttribute("priorities", FeedbackPriority.values());
        model.addAttribute("departments", departmentService.listActive());
        model.addAttribute("unreadCount", notificationService.unreadCount(principal.id()));
        return "admin/feedback-queue";
    }

    @GetMapping("/admin/students")
    public String students(@AuthenticationPrincipal AppUserDetails principal,
                           @RequestParam(required = false) String search,
                           @PageableDefault(size = 20) Pageable pageable,
                           Model model) {
        Long scope = principal.isSuperAdmin() ? null : principal.departmentId();
        model.addAttribute("page", userService.listStudents(scope, search, pageable));
        model.addAttribute("search", search);
        model.addAttribute("unreadCount", notificationService.unreadCount(principal.id()));
        return "admin/students";
    }
}
