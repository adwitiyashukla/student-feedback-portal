package com.adwitiya.feedbackportal.domain.enums;

import java.time.Duration;

/**
 * Urgency of a piece of feedback, and the resolution SLA attached to it.
 */
public enum FeedbackPriority {

    LOW(Duration.ofDays(14), 1),
    MEDIUM(Duration.ofDays(7), 2),
    HIGH(Duration.ofDays(3), 3),
    URGENT(Duration.ofHours(24), 4);

    private final Duration resolutionSla;
    private final int weight;

    FeedbackPriority(Duration resolutionSla, int weight) {
        this.resolutionSla = resolutionSla;
        this.weight = weight;
    }

    /** How long the institution has to resolve feedback at this priority. */
    public Duration getResolutionSla() {
        return resolutionSla;
    }

    /** Ordinal-independent ranking, safe to persist and compare. */
    public int getWeight() {
        return weight;
    }

    public boolean isAtLeast(FeedbackPriority other) {
        return this.weight >= other.weight;
    }

    public static FeedbackPriority fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return MEDIUM;
        }
        String normalised = label.trim().toUpperCase();
        for (FeedbackPriority priority : values()) {
            if (priority.name().equals(normalised)) {
                return priority;
            }
        }
        return MEDIUM;
    }
}
