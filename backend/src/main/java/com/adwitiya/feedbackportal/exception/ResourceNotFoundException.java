package com.adwitiya.feedbackportal.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entity, Object identifier) {
        return new ResourceNotFoundException("%s not found: %s".formatted(entity, identifier));
    }
}
