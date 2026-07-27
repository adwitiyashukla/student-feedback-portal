package com.adwitiya.feedbackportal.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Translates every exception the API can throw into an RFC 7807
 * {@code application/problem+json} response.
 *
 * <p>Two properties matter here. First, the shape is uniform, so clients parse
 * one error format. Second, internal detail never leaks: unexpected exceptions
 * are logged with a generated correlation id and the caller receives that id
 * and nothing else.</p>
 *
 * <p>Scoped to {@code web.api} on purpose. Unscoped, this advice also caught
 * failures from the Thymeleaf controllers in {@code web.ui} and answered a
 * browser navigation with a wall of raw JSON. UI errors are left to Spring
 * Boot's error handling, which resolves {@code templates/error/4xx.html} and
 * {@code templates/error/5xx.html} - Boot looks up {@code error/<status>},
 * then {@code error/<series>xx}, then a root-level {@code error}. A file at
 * {@code templates/error/error.html} matches none of those and left Tomcat's
 * stock white-on-grey page to leak through.</p>
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.adwitiya.feedbackportal.web.api")
public class GlobalExceptionHandler {

    private static final String BASE_TYPE = "https://feedback-portal/errors/";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), "not-found", request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), "conflict", request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable", ex.getMessage(), "business-rule", request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable", ex.getMessage(), "invalid-state", request);
    }

    /** Field-level bean validation failures, returned as a field-to-message map. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));

        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation Failed",
                "One or more fields are invalid.", "validation", request);
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage()));

        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Validation Failed",
                "One or more parameters are invalid.", "validation", request);
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class})
    public ProblemDetail handleMalformedRequest(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Request",
                "The request could not be parsed. Check the parameter types and JSON body.",
                "malformed-request", request);
    }

    @ExceptionHandler({BadCredentialsException.class})
    public ProblemDetail handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        // Deliberately identical whether the account exists or not.
        return problem(HttpStatus.UNAUTHORIZED, "Unauthenticated",
                "Invalid email or password.", "bad-credentials", request);
    }

    @ExceptionHandler(LockedException.class)
    public ProblemDetail handleLocked(LockedException ex, HttpServletRequest request) {
        return problem(HttpStatus.LOCKED, "Account Locked",
                "This account is temporarily locked after repeated failed sign-in attempts.",
                "account-locked", request);
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabled(DisabledException ex, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "Account Disabled",
                "This account has been disabled. Contact the administrator.", "account-disabled", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden",
                "Your account does not have permission to perform this action.", "forbidden", request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Conflict",
                "This record was modified by someone else. Reload and try again.", "stale-record", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Database constraint violated on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Conflict",
                "The request conflicts with existing data.", "data-integrity", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleUploadTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Payload Too Large",
                "The uploaded file exceeds the maximum allowed size.", "upload-too-large", request);
    }

    @ExceptionHandler(StorageException.class)
    public ProblemDetail handleStorage(StorageException ex, HttpServletRequest request) {
        log.error("Attachment storage failure on {}", request.getRequestURI(), ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Storage Unavailable",
                "The file store is temporarily unavailable. Please retry.", "storage", request);
    }

    /**
     * A path with no handler and a path with no static file are the same thing
     * to a caller: a 404. {@code NoResourceFoundException} is included so that
     * routine browser requests for {@code /favicon.ico} do not fall through to
     * the last-resort handler and fill the log with ERROR-level stack traces.
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ProblemDetail handleNoHandler(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Not Found", "No endpoint matches this path.", "not-found", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                  HttpServletRequest request) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed",
                "%s is not supported on this path.".formatted(request.getMethod()),
                "method-not-allowed", request);
    }

    /**
     * Last-resort handler. The correlation id is the only thing shared with
     * the caller; the stack trace stays in the log.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.error("Unhandled exception [{}] on {} {}", correlationId,
                request.getMethod(), request.getRequestURI(), ex);

        ProblemDetail detail = problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Something went wrong. Quote reference %s when reporting this.".formatted(correlationId),
                "internal", request);
        detail.setProperty("correlationId", correlationId);
        return detail;
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail,
                                  String typeSuffix, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create(BASE_TYPE + typeSuffix));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }
}
