package com.adwitiya.feedbackportal.web.mapper;

import com.adwitiya.feedbackportal.domain.entity.Admin;
import com.adwitiya.feedbackportal.domain.entity.Department;
import com.adwitiya.feedbackportal.domain.entity.Student;
import com.adwitiya.feedbackportal.domain.entity.User;
import com.adwitiya.feedbackportal.web.dto.response.DepartmentResponse;
import com.adwitiya.feedbackportal.web.dto.response.UserResponse;
import org.springframework.stereotype.Component;

/** Entity-to-DTO translation for accounts and departments. */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return build(user, null, null, null);
    }

    public UserResponse toResponse(Student student) {
        return build(student.getUser(), student.getDepartment(), student.getRollNumber(), null);
    }

    public UserResponse toResponse(Admin admin) {
        return build(admin.getUser(), admin.getDepartment(), null, admin.getEmployeeCode());
    }

    public DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.getDescription(),
                department.isActive());
    }

    private UserResponse build(User user, Department department, String rollNumber, String employeeCode) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.isEnabled(),
                user.isCurrentlyLocked(),
                department != null ? department.getCode() : null,
                department != null ? department.getName() : null,
                rollNumber,
                employeeCode,
                user.getLastLoginAt(),
                user.getCreatedAt());
    }
}
