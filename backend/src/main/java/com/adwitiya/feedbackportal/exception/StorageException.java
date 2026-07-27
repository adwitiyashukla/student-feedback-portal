package com.adwitiya.feedbackportal.exception;

/** Thrown when an attachment cannot be written to or read from the backing store. */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
