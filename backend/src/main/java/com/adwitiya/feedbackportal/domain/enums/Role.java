package com.adwitiya.feedbackportal.domain.enums;

/**
 * Application roles. Spring Security authorities are derived as {@code ROLE_<name>}.
 */
public enum Role {

    /** Submits feedback and tracks only their own tickets. */
    STUDENT,

    /** Handles feedback routed to their department. */
    ADMIN,

    /** Full access: user management, department management, all feedback. */
    SUPER_ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }

    public boolean isStaff() {
        return this == ADMIN || this == SUPER_ADMIN;
    }
}
