package com.adwitiya.feedbackportal.web.dto.response;

import com.adwitiya.feedbackportal.domain.enums.FeedbackCategory;
import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Everything the dashboard renders, assembled in one round trip.
 *
 * <p>{@link Serializable} because these instances are cached in Redis.</p>
 */
public record DashboardResponse(
        long totalFeedback,
        long openFeedback,
        long resolvedFeedback,
        long overdueFeedback,
        Double averageResolutionHours,
        Double averageSatisfaction,
        Map<FeedbackStatus, Long> byStatus,
        Map<FeedbackCategory, Long> byCategory,
        Map<String, Double> sentimentByCategory,
        List<TrendPoint> monthlyTrend,
        List<DepartmentPerformance> departmentPerformance
) implements Serializable {

    /** One month on the volume chart. */
    public record TrendPoint(String period, long submitted, long resolved) implements Serializable {
    }

    /** One row of the departmental league table. */
    public record DepartmentPerformance(
            Long departmentId,
            String departmentName,
            long total,
            long open,
            long resolved,
            Double averageResolutionHours,
            Double averageRating
    ) implements Serializable {
    }
}
