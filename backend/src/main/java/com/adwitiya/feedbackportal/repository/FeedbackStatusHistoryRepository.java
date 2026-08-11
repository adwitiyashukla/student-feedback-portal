package com.adwitiya.feedbackportal.repository;

import com.adwitiya.feedbackportal.domain.entity.FeedbackStatusHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackStatusHistoryRepository extends JpaRepository<FeedbackStatusHistory, Long> {
    @EntityGraph(attributePaths = {"changedBy"})
    List<FeedbackStatusHistory> findByFeedbackIdOrderByChangedAtAsc(Long feedbackId);
}
