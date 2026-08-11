package com.adwitiya.feedbackportal.util;

public final class LogSanitizer {
    private static final String CONTROL_CHARS = "[\\r\\n\\t\\p{Cntrl}]";

    private static final int MAX_LENGTH = 300;

    private LogSanitizer() {
    }

    public static String clean(String value) {
        if (value == null) {
            return "-";
        }
        String stripped = value.replaceAll(CONTROL_CHARS, "_");
        return stripped.length() <= MAX_LENGTH
                ? stripped
                : stripped.substring(0, MAX_LENGTH) + "...[truncated]";
    }
}
