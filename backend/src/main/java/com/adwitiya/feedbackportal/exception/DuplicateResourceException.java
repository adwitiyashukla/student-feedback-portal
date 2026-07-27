package com.adwitiya.feedbackportal.exception;

/** Thrown when a uniqueness constraint would be violated. Rendered as HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public static DuplicateResourceException of(String entity, String field, Object value) {
        return new DuplicateResourceException("%s with %s '%s' already exists".formatted(entity, field, value));
    }
}
