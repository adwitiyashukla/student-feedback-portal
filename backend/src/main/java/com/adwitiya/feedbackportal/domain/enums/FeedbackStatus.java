package com.adwitiya.feedbackportal.domain.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum FeedbackStatus {
    OPEN,

    IN_PROGRESS,

    AWAITING_STUDENT,

    RESOLVED,

    CLOSED,

    REJECTED;

    private static final Set<FeedbackStatus> NO_TRANSITIONS = Collections.emptySet();

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

    public boolean isTerminal() {
        return this == CLOSED || this == REJECTED;
    }

    public boolean isActive() {
        return this == OPEN || this == IN_PROGRESS || this == AWAITING_STUDENT;
    }
}
