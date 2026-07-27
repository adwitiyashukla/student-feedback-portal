package com.adwitiya.feedbackportal.util;

import java.util.regex.Pattern;

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

    /** CR, LF and the other C0 control characters, plus DEL. */
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}\\x7F]");

    /** Long values are truncated: a log line is not a place for a payload. */
    private static final int MAX_LENGTH = 300;

    private LogSanitizer() {
    }

    /**
     * @param value any caller-influenced string, may be null
     * @return the value with control characters replaced by {@code _}, truncated
     *         to a sane length, or {@code "-"} when null
     */
    public static String clean(String value) {
        if (value == null) {
            return "-";
        }
        String stripped = CONTROL_CHARS.matcher(value).replaceAll("_");
        return stripped.length() <= MAX_LENGTH
                ? stripped
                : stripped.substring(0, MAX_LENGTH) + "...[truncated]";
    }
}
