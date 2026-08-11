package com.adwitiya.feedbackportal.repository.projection;

public interface DepartmentStats {
    Long getDepartmentId();

    String getDepartmentName();

    long getTotal();

    long getOpenCount();

    long getResolvedCount();

    Double getAverageResolutionHours();

    Double getAverageRating();
}
