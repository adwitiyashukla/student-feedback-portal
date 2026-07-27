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
 * A file uploaded alongside a piece of feedback - typically a photograph of
 * the problem being reported.
 *
 * <p>Only metadata lives in MySQL. {@code storageKey} points at the bytes,
 * which sit on local disk under the {@code dev} profile and in S3 in
 * production; see {@code FileStorageService}.</p>
 */
/*
 * @CreatedDate is inert without an AuditingEntityListener. Sibling entities
 * inherit the listener from BaseEntity; this one cannot, because its table has
 * no updated_at column. Registering the listener directly is what actually
 * populates created_at - without it the insert fails on a NOT NULL column.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "attachments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;

    /** Original client-supplied name, sanitised before storage. */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /** Opaque storage location: a relative path on disk, or an S3 object key. */
    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Human-friendly size for display, e.g. {@code "1.4 MB"}. */
    public String getHumanReadableSize() {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }
        if (sizeBytes < 1024 * 1024) {
            return "%.1f KB".formatted(sizeBytes / 1024.0);
        }
        return "%.1f MB".formatted(sizeBytes / (1024.0 * 1024.0));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Attachment attachment)) {
            return false;
        }
        return id != null && id.equals(attachment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
