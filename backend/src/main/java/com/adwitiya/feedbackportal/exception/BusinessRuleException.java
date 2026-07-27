package com.adwitiya.feedbackportal.exception;

/**
 * Thrown when a request is syntactically valid but violates a domain rule —
 * an illegal workflow transition, rating an unresolved ticket, and so on.
 * Rendered as HTTP 422.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
