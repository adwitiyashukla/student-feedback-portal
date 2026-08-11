package com.adwitiya.feedbackportal.repository;

import com.adwitiya.feedbackportal.domain.entity.FeedbackComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackCommentRepository extends JpaRepository<FeedbackComment, Long> {
    @EntityGraph(attributePaths = {"author"})
    List<FeedbackComment> findByFeedbackIdOrderByCreatedAtAsc(Long feedbackId);

    @EntityGraph(attributePaths = {"author"})
    List<FeedbackComment> findByFeedbackIdAndInternalNoteFalseOrderByCreatedAtAsc(Long feedbackId);

    long countByFeedbackId(Long feedbackId);
}
