package com.adwitiya.feedbackportal.security;

import com.adwitiya.feedbackportal.domain.entity.Feedback;
import com.adwitiya.feedbackportal.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ownership checks referenced from {@code @PreAuthorize} expressions.
 */
@Component("feedbackAccess")
@RequiredArgsConstructor
public class FeedbackAccessGuard {

    private final FeedbackRepository feedbackRepository;

    /**
     * @param feedbackId the record being requested
     * @param principal  the caller
     * @return whether the caller may read this piece of feedback
     */
    @Transactional(readOnly = true)
    public boolean canView(Long feedbackId, AppUserDetails principal) {
        if (principal == null) {
            return false;
        }
        if (principal.isSuperAdmin()) {
            return true;
        }
        return feedbackRepository.findById(feedbackId)
                .map(feedback -> isOwner(feedback, principal) || isDepartmentStaff(feedback, principal))
                .orElse(false);
    }

    /** Only staff may change workflow state; students act through their own endpoints. */
    @Transactional(readOnly = true)
    public boolean canManage(Long feedbackId, AppUserDetails principal) {
        if (principal == null || !principal.isStaff()) {
            return false;
        }
        if (principal.isSuperAdmin()) {
            return true;
        }
        return feedbackRepository.findById(feedbackId)
                .map(feedback -> isDepartmentStaff(feedback, principal))
                .orElse(false);
    }

    /** The submitting student, and nobody else. */
    @Transactional(readOnly = true)
    public boolean isSubmitter(Long feedbackId, AppUserDetails principal) {
        if (principal == null) {
            return false;
        }
        return feedbackRepository.findById(feedbackId)
                .map(feedback -> isOwner(feedback, principal))
                .orElse(false);
    }

    private boolean isOwner(Feedback feedback, AppUserDetails principal) {
        return feedback.getSubmittedBy() != null
                && principal.id().equals(feedback.getSubmittedBy().getUserId());
    }

    private boolean isDepartmentStaff(Feedback feedback, AppUserDetails principal) {
        return principal.isStaff()
                && principal.departmentId() != null
                && feedback.getDepartment() != null
                && principal.departmentId().equals(feedback.getDepartment().getId());
    }
}
