package com.adwitiya.feedbackportal.repository;

import com.adwitiya.feedbackportal.domain.entity.Feedback;
import com.adwitiya.feedbackportal.domain.enums.FeedbackCategory;
import com.adwitiya.feedbackportal.domain.enums.FeedbackPriority;
import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Collection;

public final class FeedbackSpecifications {
    private FeedbackSpecifications() {
    }

    public static Specification<Feedback> all() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Feedback> hasStatus(FeedbackStatus status) {
        return status == null ? all() : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Feedback> hasStatusIn(Collection<FeedbackStatus> statuses) {
        return (statuses == null || statuses.isEmpty())
                ? all()
                : (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<Feedback> hasCategory(FeedbackCategory category) {
        return category == null ? all() : (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Feedback> hasPriority(FeedbackPriority priority) {
        return priority == null ? all() : (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Feedback> inDepartment(Long departmentId) {
        return departmentId == null
                ? all()
                : (root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId);
    }

    public static Specification<Feedback> submittedBy(Long studentUserId) {
        return studentUserId == null
                ? all()
                : (root, query, cb) -> cb.equal(root.get("submittedBy").get("userId"), studentUserId);
    }

    public static Specification<Feedback> assignedTo(Long adminUserId) {
        return adminUserId == null
                ? all()
                : (root, query, cb) -> cb.equal(root.get("assignedTo").get("userId"), adminUserId);
    }

    public static Specification<Feedback> unassigned() {
        return (root, query, cb) -> cb.isNull(root.get("assignedTo"));
    }

    public static Specification<Feedback> createdBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return cb.conjunction();
            }
            if (from == null) {
                return cb.lessThanOrEqualTo(root.get("createdAt"), to);
            }
            if (to == null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }
            return cb.between(root.get("createdAt"), from, to);
        };
    }

    public static Specification<Feedback> textContains(String term) {
        if (term == null || term.isBlank()) {
            return all();
        }
        String pattern = "%" + term.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate onTicket = cb.like(cb.lower(root.get("ticketNumber")), pattern);
            Predicate onTitle = cb.like(cb.lower(root.get("title")), pattern);
            Predicate onBody = cb.like(cb.lower(root.get("description")), pattern);
            return cb.or(onTicket, onTitle, onBody);
        };
    }

    public static Specification<Feedback> overdue(boolean onlyOverdue) {
        if (!onlyOverdue) {
            return all();
        }
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("dueAt")),
                cb.lessThan(root.get("dueAt"), cb.literal(Instant.now())),
                root.get("status").in(FeedbackStatus.OPEN, FeedbackStatus.IN_PROGRESS, FeedbackStatus.AWAITING_STUDENT));
    }
}
