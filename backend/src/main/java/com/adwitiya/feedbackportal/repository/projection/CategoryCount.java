package com.adwitiya.feedbackportal.repository.projection;

import com.adwitiya.feedbackportal.domain.enums.FeedbackCategory;

/** Aggregate row: volume and average sentiment per subject area. */
public interface CategoryCount {
    FeedbackCategory getCategory();

    long getTotal();

    Double getAverageSentiment();
}
