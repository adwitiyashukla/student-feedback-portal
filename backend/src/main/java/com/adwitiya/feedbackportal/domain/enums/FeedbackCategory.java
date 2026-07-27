package com.adwitiya.feedbackportal.domain.enums;

/**
 * Subject area of a piece of feedback.
 *
 * <p>Kept in sync with the label set the Python analytics service is trained
 * on; {@link #fromLabel(String)} is the tolerant parser used when reading a
 * suggestion back from that service.</p>
 */
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

    /**
     * Parses a category label defensively.
     *
     * @param label case-insensitive enum name, possibly {@code null}
     * @return the matching category, or {@link #OTHER} when unrecognised
     */
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
