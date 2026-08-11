package com.adwitiya.feedbackportal.domain.entity;

import com.adwitiya.feedbackportal.domain.enums.FeedbackCategory;
import com.adwitiya.feedbackportal.domain.enums.FeedbackPriority;
import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
import com.adwitiya.feedbackportal.domain.enums.SentimentLabel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "feedback")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feedback extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 24)
    private String ticketNumber;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FeedbackCategory category;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackPriority priority = FeedbackPriority.MEDIUM;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackStatus status = FeedbackStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    private Student submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private Admin assignedTo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Builder.Default
    @Column(nullable = false)
    private boolean anonymous = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment_label", length = 20)
    private SentimentLabel sentimentLabel;

    @Column(name = "sentiment_score")
    private Double sentimentScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_category", length = 30)
    private FeedbackCategory suggestedCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_priority", length = 20)
    private FeedbackPriority suggestedPriority;

    @Column(name = "analysis_confidence")
    private Double analysisConfidence;

    @Column(name = "analysed_at")
    private Instant analysedAt;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "satisfaction_rating")
    private Integer satisfactionRating;

    @Version
    @Builder.Default
    private Long version = 0L;

    @Builder.Default
    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<FeedbackComment> comments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    private List<FeedbackStatusHistory> statusHistory = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();

    public void transitionTo(FeedbackStatus target, User actor, String note) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Cannot move feedback %s from %s to %s".formatted(ticketNumber, status, target));
        }
        FeedbackStatus previous = this.status;
        this.status = target;

        Instant now = Instant.now();
        if (target == FeedbackStatus.RESOLVED) {
            this.resolvedAt = now;
        } else if (target == FeedbackStatus.CLOSED || target == FeedbackStatus.REJECTED) {
            this.closedAt = now;
            if (this.resolvedAt == null && target == FeedbackStatus.CLOSED) {
                this.resolvedAt = now;
            }
        } else if (target == FeedbackStatus.IN_PROGRESS) {
            this.resolvedAt = null;
            this.closedAt = null;
        }

        this.statusHistory.add(FeedbackStatusHistory.builder()
                .feedback(this)
                .fromStatus(previous)
                .toStatus(target)
                .changedBy(actor)
                .note(note)
                .changedAt(now)
                .build());
    }

    public void addComment(FeedbackComment comment) {
        comment.setFeedback(this);
        this.comments.add(comment);
    }

    public void addAttachment(Attachment attachment) {
        attachment.setFeedback(this);
        this.attachments.add(attachment);
    }

    public void applySla() {
        Instant base = getCreatedAt() != null ? getCreatedAt() : Instant.now();
        this.dueAt = base.plus(priority.getResolutionSla());
    }

    public boolean isOverdue() {
        return status.isActive() && dueAt != null && Instant.now().isAfter(dueAt);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Feedback feedback)) {
            return false;
        }
        return id != null && id.equals(feedback.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Feedback{ticketNumber='" + ticketNumber + "', status=" + status + "}";
    }
}
