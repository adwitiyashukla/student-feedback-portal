package com.adwitiya.feedbackportal.repository.projection;

/** Aggregate row: feedback volume per calendar month, for the trend chart. */
public interface MonthlyTrend {
    String getPeriod();

    long getSubmitted();

    long getResolved();
}
