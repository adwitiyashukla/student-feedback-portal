package com.adwitiya.feedbackportal.repository;

import com.adwitiya.feedbackportal.domain.entity.User;
import com.adwitiya.feedbackportal.domain.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(Role role);

    long countByRole(Role role);

    @Query("""
            SELECT u FROM User u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                                   OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<User> search(@Param("role") Role role, @Param("search") String search, Pageable pageable);

    @Modifying
    @Query("""
            UPDATE User u
               SET u.accountLocked = false, u.failedLoginAttempts = 0, u.lockExpiresAt = null
             WHERE u.accountLocked = true AND u.lockExpiresAt IS NOT NULL AND u.lockExpiresAt < :now
            """)
    int unlockExpiredAccounts(@Param("now") Instant now);
}
