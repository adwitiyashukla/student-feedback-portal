package com.adwitiya.feedbackportal.domain.enums;

/**
 * Categories of in-app notification, used for grouping and icon selection.
 */
public enum NotificationType {

    FEEDBACK_SUBMITTED,
    FEEDBACK_ASSIGNED,
    FEEDBACK_COMMENTED,
    FEEDBACK_STATUS_CHANGED,
    FEEDBACK_RESOLVED,
    FEEDBACK_ESCALATED,
    SLA_BREACH_WARNING,
    ACCOUNT_CREATED
}
