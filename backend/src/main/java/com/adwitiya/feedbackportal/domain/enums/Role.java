package com.adwitiya.feedbackportal.domain.enums;

public enum Role {
    STUDENT,

    ADMIN,

    SUPER_ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }

    public boolean isStaff() {
        return this == ADMIN || this == SUPER_ADMIN;
    }
}
