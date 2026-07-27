package com.adwitiya.feedbackportal.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Administrator profile, sharing its primary key with {@link User}.
 *
 * <p>The owning department is what makes routing possible: feedback filed
 * against a department is visible to that department's administrators and to
 * nobody else.</p>
 */
@Entity
@Table(name = "admins")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Admin {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "employee_code", nullable = false, unique = true, length = 30)
    private String employeeCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(length = 80)
    private String designation;

    public String getFullName() {
        return user != null ? user.getFullName() : null;
    }

    public String getEmail() {
        return user != null ? user.getEmail() : null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Admin admin)) {
            return false;
        }
        return userId != null && userId.equals(admin.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }

    @Override
    public String toString() {
        return "Admin{userId=" + userId + ", employeeCode='" + employeeCode + "'}";
    }
}
