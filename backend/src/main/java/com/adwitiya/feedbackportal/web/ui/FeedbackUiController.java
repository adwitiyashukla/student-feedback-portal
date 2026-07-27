package com.adwitiya.feedbackportal.web.ui;

import com.adwitiya.feedbackportal.security.AppUserDetails;
import com.adwitiya.feedbackportal.service.FeedbackService;
import com.adwitiya.feedbackportal.service.NotificationService;
import com.adwitiya.feedbackportal.service.UserService;
import com.adwitiya.feedbackportal.web.dto.request.AddCommentRequest;
import com.adwitiya.feedbackportal.web.dto.request.RateFeedbackRequest;
import com.adwitiya.feedbackportal.web.dto.request.UpdateFeedbackStatusRequest;
import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
import com.adwitiya.feedbackportal.exception.BusinessRuleException;
import com.adwitiya.feedbackportal.web.dto.response.FeedbackDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The feedback detail page, shared by students and staff.
 */
@Controller
@RequiredArgsConstructor
public class FeedbackUiController {

    private final FeedbackService feedbackService;
    private final UserService userService;
    private final NotificationService notificationService;

    @GetMapping("/feedback/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal AppUserDetails principal,
                         Model model) {
        FeedbackDetailResponse feedback = feedbackService.get(id, principal);

        model.addAttribute("feedback", feedback);
        model.addAttribute("commentForm", new AddCommentRequest("", false));
        model.addAttribute("unreadCount", notificationService.unreadCount(principal.id()));
        model.addAttribute("canManage", principal.isStaff());
        model.addAttribute("canRate", !principal.isStaff()
                && (feedback.status() == FeedbackStatus.RESOLVED || feedback.status() == FeedbackStatus.CLOSED)
                && feedback.satisfactionRating() == null);

        if (principal.isStaff() && feedback.departmentId() != null) {
            model.addAttribute("departmentAdmins", userService.adminsInDepartment(feedback.departmentId()));
        }
        return "feedback/detail";
    }

    @PostMapping("/feedback/{id}/comments")
    public String comment(@PathVariable Long id,
                          @RequestParam String body,
                          @RequestParam(defaultValue = "false") boolean internalNote,
                          @AuthenticationPrincipal AppUserDetails principal,
                          RedirectAttributes redirect) {
        try {
            feedbackService.addComment(id, new AddCommentRequest(body, internalNote), principal);
            redirect.addFlashAttribute("successMessage", "Reply posted.");
        } catch (BusinessRuleException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/feedback/" + id;
    }

    @PostMapping("/feedback/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam FeedbackStatus status,
                               @RequestParam(required = false) String note,
                               @AuthenticationPrincipal AppUserDetails principal,
                               RedirectAttributes redirect) {
        try {
            feedbackService.changeStatus(id, new UpdateFeedbackStatusRequest(status, note), principal);
            redirect.addFlashAttribute("successMessage", "Status updated to " + status + ".");
        } catch (BusinessRuleException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/feedback/" + id;
    }

    @PostMapping("/feedback/{id}/assignee")
    public String assign(@PathVariable Long id,
                         @RequestParam Long adminUserId,
                         @AuthenticationPrincipal AppUserDetails principal,
                         RedirectAttributes redirect) {
        try {
            feedbackService.assign(id, adminUserId, principal);
            redirect.addFlashAttribute("successMessage", "Ticket reassigned.");
        } catch (BusinessRuleException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/feedback/" + id;
    }

    @PostMapping("/feedback/{id}/rating")
    public String rate(@PathVariable Long id,
                       @RequestParam Integer rating,
                       @AuthenticationPrincipal AppUserDetails principal,
                       RedirectAttributes redirect) {
        try {
            feedbackService.rate(id, new RateFeedbackRequest(rating), principal);
            redirect.addFlashAttribute("successMessage", "Thanks for the rating.");
        } catch (BusinessRuleException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/feedback/" + id;
    }
}
