package com.adwitiya.feedbackportal.web.ui;

import com.adwitiya.feedbackportal.domain.enums.FeedbackCategory;
import com.adwitiya.feedbackportal.security.AppUserDetails;
import com.adwitiya.feedbackportal.service.DepartmentService;
import com.adwitiya.feedbackportal.service.FeedbackService;
import com.adwitiya.feedbackportal.service.NotificationService;
import com.adwitiya.feedbackportal.web.dto.request.CreateFeedbackRequest;
import com.adwitiya.feedbackportal.web.dto.request.FeedbackFilterRequest;
import com.adwitiya.feedbackportal.web.dto.response.FeedbackDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentUiController {
    private final FeedbackService feedbackService;
    private final DepartmentService departmentService;
    private final NotificationService notificationService;

    @GetMapping("/student/dashboard")
    public String dashboard(@AuthenticationPrincipal AppUserDetails principal,
                            @ModelAttribute FeedbackFilterRequest filter,
                            @PageableDefault(size = 10, sort = "createdAt",
                                    direction = Sort.Direction.DESC) Pageable pageable,
                            Model model) {
        model.addAttribute("page", feedbackService.list(filter, principal, pageable));
        model.addAttribute("filter", filter);
        model.addAttribute("activeCount", feedbackService.countActiveForStudent(principal.id()));
        model.addAttribute("notifications", notificationService.recent(principal.id()));
        model.addAttribute("unreadCount", notificationService.unreadCount(principal.id()));
        model.addAttribute("statuses", com.adwitiya.feedbackportal.domain.enums.FeedbackStatus.values());
        return "student/dashboard";
    }

    @GetMapping("/student/feedback/new")
    public String newFeedbackForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new CreateFeedbackRequest("", "", null, null, false));
        }
        model.addAttribute("departments", departmentService.listActive());
        model.addAttribute("categories", FeedbackCategory.values());
        return "student/new-feedback";
    }

    @PostMapping("/student/feedback")
    public String submitFeedback(@Valid @ModelAttribute("form") CreateFeedbackRequest form,
                                 BindingResult binding,
                                 @AuthenticationPrincipal AppUserDetails principal,
                                 Model model,
                                 RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("departments", departmentService.listActive());
            model.addAttribute("categories", FeedbackCategory.values());
            return "student/new-feedback";
        }

        FeedbackDetailResponse created = feedbackService.submit(form, principal);
        redirect.addFlashAttribute("successMessage",
                "Feedback submitted as " + created.ticketNumber() + ".");
        return "redirect:/feedback/" + created.id();
    }

    @PostMapping("/student/notifications/read")
    public String markNotificationsRead(@AuthenticationPrincipal AppUserDetails principal,
                                        @RequestParam(defaultValue = "/student/dashboard") String returnTo) {
        notificationService.markAllRead(principal.id());

        return "redirect:" + (returnTo.startsWith("/") ? returnTo : "/student/dashboard");
    }
}
