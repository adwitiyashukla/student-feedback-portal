package com.adwitiya.feedbackportal.repository.projection;

import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;

/** Aggregate row: how many pieces of feedback sit in each workflow state. */
public interface StatusCount {
    FeedbackStatus getStatus();

    long getTotal();
}
