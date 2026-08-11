package com.adwitiya.feedbackportal.service;

import com.adwitiya.feedbackportal.config.CacheConfig;
import com.adwitiya.feedbackportal.domain.enums.FeedbackCategory;
import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
import com.adwitiya.feedbackportal.repository.FeedbackRepository;
import com.adwitiya.feedbackportal.repository.projection.CategoryCount;
import com.adwitiya.feedbackportal.repository.projection.DepartmentStats;
import com.adwitiya.feedbackportal.repository.projection.MonthlyTrend;
import com.adwitiya.feedbackportal.repository.projection.StatusCount;
import com.adwitiya.feedbackportal.web.dto.response.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private static final List<FeedbackStatus> ACTIVE =
            List.of(FeedbackStatus.OPEN, FeedbackStatus.IN_PROGRESS, FeedbackStatus.AWAITING_STUDENT);

    private final FeedbackRepository feedbackRepository;

    @Cacheable(value = CacheConfig.CACHE_DASHBOARD, key = "#departmentId == null ? 'all' : #departmentId")
    @Transactional(readOnly = true)
    public DashboardResponse build(Long departmentId) {
        Map<FeedbackStatus, Long> byStatus = new EnumMap<>(FeedbackStatus.class);
        for (StatusCount row : feedbackRepository.countByStatusGrouped(departmentId)) {
            byStatus.put(row.getStatus(), row.getTotal());
        }

        Map<FeedbackCategory, Long> byCategory = new EnumMap<>(FeedbackCategory.class);
        Map<String, Double> sentimentByCategory = new java.util.LinkedHashMap<>();
        for (CategoryCount row : feedbackRepository.countByCategoryGrouped(departmentId)) {
            byCategory.put(row.getCategory(), row.getTotal());
            if (row.getAverageSentiment() != null) {
                sentimentByCategory.put(row.getCategory().name(), round(row.getAverageSentiment()));
            }
        }

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long open = ACTIVE.stream().mapToLong(status -> byStatus.getOrDefault(status, 0L)).sum();
        long resolved = byStatus.getOrDefault(FeedbackStatus.RESOLVED, 0L)
                + byStatus.getOrDefault(FeedbackStatus.CLOSED, 0L);
        long overdue = feedbackRepository.findOverdue(Instant.now(), PageRequest.of(0, 1)).getTotalElements();

        return new DashboardResponse(
                total,
                open,
                resolved,
                overdue,
                round(feedbackRepository.averageResolutionHours(departmentId)),
                round(feedbackRepository.averageSatisfaction(departmentId)),
                byStatus,
                byCategory,
                sentimentByCategory,
                monthlyTrend(departmentId),
                departmentPerformance(departmentId));
    }

    @Cacheable(value = CacheConfig.CACHE_TRENDS, key = "#departmentId == null ? 'all' : #departmentId")
    @Transactional(readOnly = true)
    public List<DashboardResponse.TrendPoint> monthlyTrend(Long departmentId) {
        Instant since = Instant.now().minus(365, ChronoUnit.DAYS);
        return feedbackRepository.monthlyTrend(since, departmentId).stream()
                .map(this::toTrendPoint)
                .toList();
    }

    private DashboardResponse.TrendPoint toTrendPoint(MonthlyTrend row) {
        return new DashboardResponse.TrendPoint(row.getPeriod(), row.getSubmitted(), row.getResolved());
    }

    private List<DashboardResponse.DepartmentPerformance> departmentPerformance(Long departmentId) {
        List<DepartmentStats> rows = feedbackRepository.aggregateByDepartment();
        return rows.stream()
                .filter(row -> departmentId == null || departmentId.equals(row.getDepartmentId()))
                .map(row -> new DashboardResponse.DepartmentPerformance(
                        row.getDepartmentId(),
                        row.getDepartmentName(),
                        row.getTotal(),
                        row.getOpenCount(),
                        row.getResolvedCount(),
                        round(row.getAverageResolutionHours()),
                        round(row.getAverageRating())))
                .toList();
    }

    private Double round(Double value) {
        return value == null ? null : Math.round(value * 100.0) / 100.0;
    }
}
