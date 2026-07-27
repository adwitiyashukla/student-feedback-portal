package com.adwitiya.feedbackportal.domain.entity;

import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * Append-only record of one workflow transition.
 *
 * <p>Rows are never updated or deleted, so the full history of who moved a
 * ticket and when survives even if the ticket itself is later reassigned.</p>
 */
@Entity
@Table(name = "feedback_status_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private FeedbackStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private FeedbackStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    @Column(length = 500)
    private String note;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FeedbackStatusHistory history)) {
            return false;
        }
        return id != null && id.equals(history.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
