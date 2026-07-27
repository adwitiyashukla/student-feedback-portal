package com.adwitiya.feedbackportal.repository.projection;

/** Aggregate row driving the departmental performance table on the admin dashboard. */
public interface DepartmentStats {
    Long getDepartmentId();

    String getDepartmentName();

    long getTotal();

    long getOpenCount();

    long getResolvedCount();

    Double getAverageResolutionHours();

    Double getAverageRating();
}
