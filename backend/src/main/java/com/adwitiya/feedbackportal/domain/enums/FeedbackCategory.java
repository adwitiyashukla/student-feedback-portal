package com.adwitiya.feedbackportal.domain.enums;

public enum FeedbackCategory {
    ACADEMIC("Academic & Curriculum"),
    FACULTY("Faculty & Teaching"),
    EXAMINATION("Examination & Results"),
    INFRASTRUCTURE("Infrastructure & Facilities"),
    HOSTEL("Hostel & Mess"),
    LIBRARY("Library"),
    TRANSPORT("Transport"),
    ADMINISTRATION("Administration & Fees"),
    IT_SUPPORT("IT & Network"),
    OTHER("Other");

    private final String displayName;

    FeedbackCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static FeedbackCategory fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return OTHER;
        }
        String normalised = label.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        for (FeedbackCategory category : values()) {
            if (category.name().equals(normalised)) {
                return category;
            }
        }
        return OTHER;
    }
}
