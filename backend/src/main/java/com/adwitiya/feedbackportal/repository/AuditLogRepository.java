package com.adwitiya.feedbackportal.repository;

import com.adwitiya.feedbackportal.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:action IS NULL OR a.action = :action)
              AND (:actorEmail IS NULL OR LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :actorEmail, '%')))
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> search(@Param("action") String action,
                          @Param("actorEmail") String actorEmail,
                          Pageable pageable);
}
