package com.adwitiya.feedbackportal.domain.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Workflow state of a piece of feedback.
 */
public enum FeedbackStatus {

    /** Submitted, not yet picked up. */
    OPEN,

    /** An administrator is actively working on it. */
    IN_PROGRESS,

    /** Blocked pending more information from the student. */
    AWAITING_STUDENT,

    /** A resolution has been offered; the student may still reopen it. */
    RESOLVED,

    /** Terminal: accepted by the student or auto-closed after the grace period. */
    CLOSED,

    /** Terminal: out of scope, duplicate or invalid. */
    REJECTED;

    private static final Set<FeedbackStatus> NO_TRANSITIONS = Collections.emptySet();

    /** States reachable from this one. */
    public Set<FeedbackStatus> allowedTransitions() {
        return switch (this) {
            case OPEN -> EnumSet.of(IN_PROGRESS, AWAITING_STUDENT, RESOLVED, REJECTED);
            case IN_PROGRESS -> EnumSet.of(AWAITING_STUDENT, RESOLVED, REJECTED);
            case AWAITING_STUDENT -> EnumSet.of(IN_PROGRESS, RESOLVED, REJECTED);
            case RESOLVED -> EnumSet.of(CLOSED, IN_PROGRESS);
            case CLOSED, REJECTED -> NO_TRANSITIONS;
        };
    }

    public boolean canTransitionTo(FeedbackStatus target) {
        return target != null && allowedTransitions().contains(target);
    }

    /** True when no further transition is possible. */
    public boolean isTerminal() {
        return this == CLOSED || this == REJECTED;
    }

    /** True while the ticket still counts against the department's open workload. */
    public boolean isActive() {
        return this == OPEN || this == IN_PROGRESS || this == AWAITING_STUDENT;
    }
}
