package com.adwitiya.feedbackportal.repository;

import com.adwitiya.feedbackportal.domain.entity.Feedback;
import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
import com.adwitiya.feedbackportal.repository.projection.CategoryCount;
import com.adwitiya.feedbackportal.repository.projection.DepartmentStats;
import com.adwitiya.feedbackportal.repository.projection.MonthlyTrend;
import com.adwitiya.feedbackportal.repository.projection.StatusCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Feedback persistence, including the aggregate queries behind the dashboards.
 */
@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long>, JpaSpecificationExecutor<Feedback> {

    @EntityGraph(attributePaths = {"submittedBy", "submittedBy.user", "assignedTo", "assignedTo.user", "department"})
    Optional<Feedback> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"submittedBy", "submittedBy.user", "assignedTo", "assignedTo.user", "department"})
    Optional<Feedback> findByTicketNumber(String ticketNumber);

    boolean existsByTicketNumber(String ticketNumber);

    long countBySubmittedByUserId(Long userId);

    long countByStatus(FeedbackStatus status);

    long countByStatusIn(List<FeedbackStatus> statuses);

    long countByAssignedToUserIdAndStatusIn(Long adminUserId, List<FeedbackStatus> statuses);

    long countByDepartmentIdAndStatusIn(Long departmentId, List<FeedbackStatus> statuses);

    /** Highest ticket sequence issued in a given year, used to mint the next one. */
    @Query(value = "SELECT MAX(CAST(SUBSTRING(f.ticket_number, 9) AS UNSIGNED)) "
            + "FROM feedback f WHERE f.ticket_number LIKE CONCAT('FB-', :year, '-%')",
            nativeQuery = true)
    Optional<Long> findMaxTicketSequenceForYear(@Param("year") String year);

    // ------------------------------------------------------------------
    //  Dashboard aggregates
    // ------------------------------------------------------------------

    @Query("""
            SELECT f.status AS status, COUNT(f) AS total
              FROM Feedback f
             WHERE (:departmentId IS NULL OR f.department.id = :departmentId)
             GROUP BY f.status
            """)
    List<StatusCount> countByStatusGrouped(@Param("departmentId") Long departmentId);

    @Query("""
            SELECT f.category AS category, COUNT(f) AS total, AVG(f.sentimentScore) AS averageSentiment
              FROM Feedback f
             WHERE (:departmentId IS NULL OR f.department.id = :departmentId)
             GROUP BY f.category
             ORDER BY COUNT(f) DESC
            """)
    List<CategoryCount> countByCategoryGrouped(@Param("departmentId") Long departmentId);

    @Query("""
            SELECT d.id AS departmentId,
                   d.name AS departmentName,
                   COUNT(f) AS total,
                   SUM(CASE WHEN f.status IN (com.adwitiya.feedbackportal.domain.enums.FeedbackStatus.OPEN,
                                              com.adwitiya.feedbackportal.domain.enums.FeedbackStatus.IN_PROGRESS,
                                              com.adwitiya.feedbackportal.domain.enums.FeedbackStatus.AWAITING_STUDENT)
                             THEN 1 ELSE 0 END) AS openCount,
                   SUM(CASE WHEN f.resolvedAt IS NOT NULL THEN 1 ELSE 0 END) AS resolvedCount,
                   AVG(CASE WHEN f.resolvedAt IS NOT NULL
                            THEN CAST(FUNCTION('TIMESTAMPDIFF', HOUR, f.createdAt, f.resolvedAt) AS Long)
                            ELSE NULL END) AS averageResolutionHours,
                   AVG(f.satisfactionRating) AS averageRating
              FROM Feedback f
              JOIN f.department d
             GROUP BY d.id, d.name
             ORDER BY COUNT(f) DESC
            """)
    List<DepartmentStats> aggregateByDepartment();

    @Query(value = """
            SELECT DATE_FORMAT(f.created_at, '%Y-%m')                                  AS period,
                   COUNT(*)                                                            AS submitted,
                   SUM(CASE WHEN f.resolved_at IS NOT NULL THEN 1 ELSE 0 END)          AS resolved
              FROM feedback f
             WHERE f.created_at >= :since
               AND (:departmentId IS NULL OR f.department_id = :departmentId)
             GROUP BY DATE_FORMAT(f.created_at, '%Y-%m')
             ORDER BY period ASC
            """, nativeQuery = true)
    List<MonthlyTrend> monthlyTrend(@Param("since") Instant since,
                                    @Param("departmentId") Long departmentId);

    @Query("""
            SELECT AVG(CAST(FUNCTION('TIMESTAMPDIFF', HOUR, f.createdAt, f.resolvedAt) AS Long))
              FROM Feedback f
             WHERE f.resolvedAt IS NOT NULL
               AND (:departmentId IS NULL OR f.department.id = :departmentId)
            """)
    Double averageResolutionHours(@Param("departmentId") Long departmentId);

    @Query("""
            SELECT AVG(f.satisfactionRating) FROM Feedback f
             WHERE f.satisfactionRating IS NOT NULL
               AND (:departmentId IS NULL OR f.department.id = :departmentId)
            """)
    Double averageSatisfaction(@Param("departmentId") Long departmentId);

    /** Active tickets past their SLA deadline, newest breach first. */
    @EntityGraph(attributePaths = {"department", "assignedTo", "assignedTo.user"})
    @Query("""
            SELECT f FROM Feedback f
             WHERE f.dueAt < :now
               AND f.status IN (com.adwitiya.feedbackportal.domain.enums.FeedbackStatus.OPEN,
                                com.adwitiya.feedbackportal.domain.enums.FeedbackStatus.IN_PROGRESS,
                                com.adwitiya.feedbackportal.domain.enums.FeedbackStatus.AWAITING_STUDENT)
             ORDER BY f.dueAt ASC
            """)
    Page<Feedback> findOverdue(@Param("now") Instant now, Pageable pageable);

    /** RESOLVED tickets the student never acknowledged; the scheduler closes these. */
    @Query("""
            SELECT f FROM Feedback f
             WHERE f.status = com.adwitiya.feedbackportal.domain.enums.FeedbackStatus.RESOLVED
               AND f.resolvedAt < :cutoff
            """)
    List<Feedback> findResolvedBefore(@Param("cutoff") Instant cutoff);

    /**
     * MySQL full-text search over title and description.
     *
     * <p>Uses the {@code ft_feedback_text} index created in V1; a
     * {@code LIKE '%term%'} scan cannot use an index at all.</p>
     */
    @Query(value = """
            SELECT * FROM feedback f
             WHERE MATCH(f.title, f.description) AGAINST (:term IN NATURAL LANGUAGE MODE)
            """,
            countQuery = """
            SELECT COUNT(*) FROM feedback f
             WHERE MATCH(f.title, f.description) AGAINST (:term IN NATURAL LANGUAGE MODE)
            """,
            nativeQuery = true)
    Page<Feedback> fullTextSearch(@Param("term") String term, Pageable pageable);
}
