package com.adwitiya.feedbackportal.domain.enums;

public enum SentimentLabel {
    POSITIVE,
    NEUTRAL,
    NEGATIVE;

    public static SentimentLabel fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return NEUTRAL;
        }
        String normalised = label.trim().toUpperCase();
        for (SentimentLabel sentiment : values()) {
            if (sentiment.name().equals(normalised)) {
                return sentiment;
            }
        }
        return NEUTRAL;
    }
}
