package com.adwitiya.feedbackportal.web.mapper;

import com.adwitiya.feedbackportal.domain.entity.Attachment;
import com.adwitiya.feedbackportal.domain.entity.Feedback;
import com.adwitiya.feedbackportal.domain.entity.FeedbackComment;
import com.adwitiya.feedbackportal.domain.entity.FeedbackStatusHistory;
import com.adwitiya.feedbackportal.web.dto.response.AttachmentResponse;
import com.adwitiya.feedbackportal.web.dto.response.CommentResponse;
import com.adwitiya.feedbackportal.web.dto.response.FeedbackDetailResponse;
import com.adwitiya.feedbackportal.web.dto.response.FeedbackSummaryResponse;
import com.adwitiya.feedbackportal.web.dto.response.StatusHistoryResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Entity-to-DTO translation for the feedback aggregate.
 *
 * <p>Hand-written rather than generated: the anonymity rule below is a
 * business decision that belongs in reviewable code, not in a mapping
 * annotation.</p>
 */
@Component
public class FeedbackMapper {

    private static final String ANONYMOUS_LABEL = "Anonymous student";
    private static final String UNASSIGNED_LABEL = "Unassigned";

    public FeedbackSummaryResponse toSummary(Feedback feedback) {
        return new FeedbackSummaryResponse(
                feedback.getId(),
                feedback.getTicketNumber(),
                feedback.getTitle(),
                feedback.getCategory(),
                feedback.getPriority(),
                feedback.getStatus(),
                feedback.getDepartment() != null ? feedback.getDepartment().getName() : null,
                submitterName(feedback),
                assigneeName(feedback),
                feedback.getSentimentLabel(),
                feedback.isOverdue(),
                feedback.getDueAt(),
                feedback.getCreatedAt());
    }

    /**
     * @param feedback     the aggregate root
     * @param comments     thread already filtered for the caller's visibility
     * @param history      workflow transitions
     * @param attachments  uploaded files
     * @param includeIdentity whether the caller may see an anonymous submitter's name
     */
    public FeedbackDetailResponse toDetail(Feedback feedback,
                                           List<FeedbackComment> comments,
                                           List<FeedbackStatusHistory> history,
                                           List<Attachment> attachments,
                                           boolean includeIdentity) {
        boolean hideIdentity = feedback.isAnonymous() && !includeIdentity;

        return new FeedbackDetailResponse(
                feedback.getId(),
                feedback.getTicketNumber(),
                feedback.getTitle(),
                feedback.getDescription(),
                feedback.getCategory(),
                feedback.getPriority(),
                feedback.getStatus(),
                feedback.getStatus().allowedTransitions(),
                feedback.getDepartment() != null ? feedback.getDepartment().getName() : null,
                feedback.getDepartment() != null ? feedback.getDepartment().getId() : null,
                hideIdentity ? ANONYMOUS_LABEL : submitterName(feedback),
                hideIdentity ? null : (feedback.getSubmittedBy() != null
                        ? feedback.getSubmittedBy().getRollNumber() : null),
                feedback.isAnonymous(),
                assigneeName(feedback),
                feedback.getAssignedTo() != null ? feedback.getAssignedTo().getUserId() : null,
                feedback.getSentimentLabel(),
                feedback.getSentimentScore(),
                feedback.getSuggestedCategory(),
                feedback.getSuggestedPriority(),
                feedback.getAnalysisConfidence(),
                feedback.getSatisfactionRating(),
                feedback.isOverdue(),
                feedback.getDueAt(),
                feedback.getResolvedAt(),
                feedback.getClosedAt(),
                feedback.getCreatedAt(),
                feedback.getUpdatedAt(),
                comments.stream().map(this::toComment).toList(),
                history.stream().map(this::toHistory).toList(),
                attachments.stream().map(this::toAttachment).toList());
    }

    public CommentResponse toComment(FeedbackComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthor() != null ? comment.getAuthor().getFullName() : null,
                comment.getAuthor() != null ? comment.getAuthor().getRole() : null,
                comment.getBody(),
                comment.isInternalNote(),
                comment.getCreatedAt());
    }

    public StatusHistoryResponse toHistory(FeedbackStatusHistory history) {
        return new StatusHistoryResponse(
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy() != null ? history.getChangedBy().getFullName() : null,
                history.getNote(),
                history.getChangedAt());
    }

    public AttachmentResponse toAttachment(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getHumanReadableSize(),
                "/api/v1/feedback/%d/attachments/%d".formatted(
                        attachment.getFeedback().getId(), attachment.getId()),
                attachment.getCreatedAt());
    }

    private String submitterName(Feedback feedback) {
        if (feedback.isAnonymous()) {
            return ANONYMOUS_LABEL;
        }
        return feedback.getSubmittedBy() != null ? feedback.getSubmittedBy().getFullName() : null;
    }

    private String assigneeName(Feedback feedback) {
        return feedback.getAssignedTo() != null ? feedback.getAssignedTo().getFullName() : UNASSIGNED_LABEL;
    }
}
