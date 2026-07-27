package com.adwitiya.feedbackportal.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

/**
 * One message in the conversation attached to a piece of feedback.
 *
 * <p>An {@code internalNote} is visible only to staff, which lets
 * administrators coordinate without exposing working notes to the student.</p>
 */
/*
 * @CreatedDate is inert without an AuditingEntityListener. Sibling entities
 * inherit the listener from BaseEntity; this one cannot, because its table has
 * no updated_at column. Registering the listener directly is what actually
 * populates created_at - without it the insert fails on a NOT NULL column.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "feedback_comments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Builder.Default
    @Column(name = "internal_note", nullable = false)
    private boolean internalNote = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FeedbackComment comment)) {
            return false;
        }
        return id != null && id.equals(comment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
