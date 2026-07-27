package com.adwitiya.feedbackportal.util;

/**
 * Neutralises caller-controlled text before it reaches a log line.
 *
 * <p>Request URIs, HTTP methods, header values and exception messages can all
 * carry whatever a caller puts in them, including CR and LF. Writing those
 * straight into a log lets an attacker inject line breaks and forge entries
 * that look like they came from the application, which makes the log
 * untrustworthy exactly when it matters. Control characters are replaced
 * rather than dropped so the tampering attempt stays visible.</p>
 */
public final class LogSanitizer {

    /**
     * Deliberately a compile-time constant passed to {@link String#replaceAll},
     * rather than a precompiled {@link java.util.regex.Pattern}. Static analysis
     * recognises this exact shape as a log-injection barrier; the equivalent
     * {@code Pattern.matcher(s).replaceAll(...)} is a different method and is
     * not recognised, so the finding survives a fix that actually works.
     */
    private static final String CONTROL_CHARS = "[\\r\\n\\t\\p{Cntrl}]";

    /** Long values are truncated: a log line is not a place for a payload. */
    private static final int MAX_LENGTH = 300;

    private LogSanitizer() {
    }

    /**
     * @param value any caller-influenced string, may be null
     * @return the value with CR, LF and other control characters replaced by
     *         {@code _}, truncated to a sane length, or {@code "-"} when null
     */
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
