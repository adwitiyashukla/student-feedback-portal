package com.adwitiya.feedbackportal.repository;

import com.adwitiya.feedbackportal.domain.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    @EntityGraph(attributePaths = {"user", "department"})
    Optional<Student> findByRollNumberIgnoreCase(String rollNumber);

    @EntityGraph(attributePaths = {"user", "department"})
    Optional<Student> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "department"})
    Optional<Student> findByUserEmailIgnoreCase(String email);

    boolean existsByRollNumberIgnoreCase(String rollNumber);

    @EntityGraph(attributePaths = {"user", "department"})
    @Query("""
            SELECT s FROM Student s
            WHERE (:departmentId IS NULL OR s.department.id = :departmentId)
              AND (:search IS NULL OR LOWER(s.rollNumber)   LIKE LOWER(CONCAT('%', :search, '%'))
                                   OR LOWER(s.user.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                                   OR LOWER(s.user.email)    LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Student> search(@Param("departmentId") Long departmentId,
                         @Param("search") String search,
                         Pageable pageable);
}
