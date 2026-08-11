package com.adwitiya.feedbackportal.repository.projection;

import com.adwitiya.feedbackportal.domain.enums.FeedbackCategory;

public interface CategoryCount {
    FeedbackCategory getCategory();

    long getTotal();

    Double getAverageSentiment();
}
