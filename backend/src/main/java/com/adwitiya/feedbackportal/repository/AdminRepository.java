package com.adwitiya.feedbackportal.repository;

import com.adwitiya.feedbackportal.domain.entity.Admin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    @EntityGraph(attributePaths = {"user", "department"})
    Optional<Admin> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "department"})
    Optional<Admin> findByUserEmailIgnoreCase(String email);

    boolean existsByEmployeeCodeIgnoreCase(String employeeCode);

    @EntityGraph(attributePaths = {"user", "department"})
    List<Admin> findByDepartmentIdAndUserEnabledTrue(Long departmentId);

    @EntityGraph(attributePaths = {"user", "department"})
    Page<Admin> findAllBy(Pageable pageable);

    @Query("""
            SELECT a.userId FROM Admin a
             WHERE a.department.id = :departmentId AND a.user.enabled = true
             ORDER BY (
                 SELECT COUNT(f) FROM Feedback f
                  WHERE f.assignedTo = a
                    AND f.status IN (com.adwitiya.feedbackportal.domain.enums.FeedbackStatus.OPEN,
                                     com.adwitiya.feedbackportal.domain.enums.FeedbackStatus.IN_PROGRESS,
                                     com.adwitiya.feedbackportal.domain.enums.FeedbackStatus.AWAITING_STUDENT)
             ) ASC, a.userId ASC
            """)
    List<Long> findLeastLoadedAdminIds(@Param("departmentId") Long departmentId, Pageable pageable);
}
