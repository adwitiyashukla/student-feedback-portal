package com.adwitiya.feedbackportal.repository.projection;

import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;

public interface StatusCount {
    FeedbackStatus getStatus();

    long getTotal();
}
