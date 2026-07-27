package com.adwitiya.feedbackportal.domain.entity;

import com.adwitiya.feedbackportal.domain.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

/**
 * An in-app notification delivered to a single user.
 */
/*
 * @CreatedDate is inert without an AuditingEntityListener. Sibling entities
 * inherit the listener from BaseEntity; this one cannot, because its table has
 * no updated_at column. Registering the listener directly is what actually
 * populates created_at - without it the insert fails on a NOT NULL column.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    /** Relative URL the notification points at, e.g. {@code /feedback/42}. */
    @Column(length = 255)
    private String link;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Notification notification)) {
            return false;
        }
        return id != null && id.equals(notification.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
