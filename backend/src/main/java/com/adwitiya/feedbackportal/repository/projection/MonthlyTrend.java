package com.adwitiya.feedbackportal.repository.projection;

public interface MonthlyTrend {
    String getPeriod();

    long getSubmitted();

    long getResolved();
}
