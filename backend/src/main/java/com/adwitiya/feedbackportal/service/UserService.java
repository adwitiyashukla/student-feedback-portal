package com.adwitiya.feedbackportal.service;

import com.adwitiya.feedbackportal.domain.entity.Admin;
import com.adwitiya.feedbackportal.domain.entity.Department;
import com.adwitiya.feedbackportal.domain.entity.Student;
import com.adwitiya.feedbackportal.domain.entity.User;
import com.adwitiya.feedbackportal.domain.enums.NotificationType;
import com.adwitiya.feedbackportal.domain.enums.Role;
import com.adwitiya.feedbackportal.exception.BusinessRuleException;
import com.adwitiya.feedbackportal.exception.DuplicateResourceException;
import com.adwitiya.feedbackportal.exception.ResourceNotFoundException;
import com.adwitiya.feedbackportal.repository.AdminRepository;
import com.adwitiya.feedbackportal.repository.DepartmentRepository;
import com.adwitiya.feedbackportal.repository.RefreshTokenRepository;
import com.adwitiya.feedbackportal.repository.StudentRepository;
import com.adwitiya.feedbackportal.repository.UserRepository;
import com.adwitiya.feedbackportal.web.dto.request.CreateAdminRequest;
import com.adwitiya.feedbackportal.web.dto.request.CreateStudentRequest;
import com.adwitiya.feedbackportal.web.dto.request.UpdateUserRequest;
import com.adwitiya.feedbackportal.web.dto.response.PageResponse;
import com.adwitiya.feedbackportal.web.dto.response.UserResponse;
import com.adwitiya.feedbackportal.web.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;
    private final DepartmentRepository departmentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final UserMapper mapper;

    @Transactional
    public UserResponse createStudent(CreateStudentRequest request) {
        String email = normalise(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw DuplicateResourceException.of("Account", "email", email);
        }
        if (studentRepository.existsByRollNumberIgnoreCase(request.rollNumber())) {
            throw DuplicateResourceException.of("Student", "roll number", request.rollNumber());
        }
        Department department = requireDepartment(request.departmentId());

        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .phone(blankToNull(request.phone()))
                .role(Role.STUDENT)
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .version(0L)
                .build());

        Student student = studentRepository.save(Student.builder()
                .user(user)
                .rollNumber(request.rollNumber().trim().toUpperCase())
                .department(department)
                .program(blankToNull(request.program()))
                .batchYear(request.batchYear())
                .semester(request.semester())
                .build());

        auditService.record("STUDENT_CREATED", "User", user.getId(), email);
        notificationService.notifyUser(user, NotificationType.ACCOUNT_CREATED,
                "Welcome to the Student Feedback Portal",
                "Your account has been created. Sign in with your university email address.", "/student/dashboard");

        return mapper.toResponse(student);
    }

    @Transactional
    public UserResponse createAdmin(CreateAdminRequest request) {
        String email = normalise(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw DuplicateResourceException.of("Account", "email", email);
        }
        if (adminRepository.existsByEmployeeCodeIgnoreCase(request.employeeCode())) {
            throw DuplicateResourceException.of("Administrator", "employee code", request.employeeCode());
        }
        if (request.role() == Role.STUDENT) {
            throw new BusinessRuleException("Use the student endpoint to create student accounts");
        }
        Department department = requireDepartment(request.departmentId());

        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .phone(blankToNull(request.phone()))
                .role(request.role())
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .version(0L)
                .build());

        Admin admin = adminRepository.save(Admin.builder()
                .user(user)
                .employeeCode(request.employeeCode().trim().toUpperCase())
                .department(department)
                .designation(blankToNull(request.designation()))
                .build());

        auditService.record("ADMIN_CREATED", "User", user.getId(), "%s (%s)".formatted(email, request.role()));
        return mapper.toResponse(admin);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listStudents(Long departmentId, String search, Pageable pageable) {
        return PageResponse.from(
                studentRepository.search(departmentId, blankToNull(search), pageable),
                mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listAdmins(Pageable pageable) {
        return PageResponse.from(adminRepository.findAllBy(pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> adminsInDepartment(Long departmentId) {
        return adminRepository.findByDepartmentIdAndUserEnabledTrue(departmentId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        if (user.getRole().isStaff()) {
            return adminRepository.findByUserId(userId).map(mapper::toResponse)
                    .orElseGet(() -> mapper.toResponse(user));
        }
        return studentRepository.findByUserId(userId).map(mapper::toResponse)
                .orElseGet(() -> mapper.toResponse(user));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        user.setFullName(request.fullName().trim());
        user.setPhone(blankToNull(request.phone()));
        userRepository.save(user);
        auditService.record("PROFILE_UPDATED", "User", userId, null);

        return get(userId);
    }

    @Transactional
    public UserResponse setEnabled(Long userId, boolean enabled, Long actingUserId) {
        if (userId.equals(actingUserId)) {
            throw new BusinessRuleException("You cannot disable your own account");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        if (!enabled && user.getRole() == Role.SUPER_ADMIN && userRepository.countByRole(Role.SUPER_ADMIN) <= 1) {
            throw new BusinessRuleException("The last super-administrator cannot be disabled");
        }

        user.setEnabled(enabled);
        if (enabled) {
            user.setAccountLocked(false);
            user.setFailedLoginAttempts(0);
            user.setLockExpiresAt(null);
        } else {
            refreshTokenRepository.revokeAllForUser(userId);
        }
        userRepository.save(user);
        auditService.record(enabled ? "ACCOUNT_ENABLED" : "ACCOUNT_DISABLED", "User", userId, null);

        return get(userId);
    }

    @Transactional
    public UserResponse unlock(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockExpiresAt(null);
        userRepository.save(user);
        auditService.record("ACCOUNT_UNLOCKED", "User", userId, null);
        return get(userId);
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(userId);
        auditService.record("PASSWORD_RESET", "User", userId, "Reset by administrator");
    }

    private Department requireDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Department", departmentId));
    }

    private String normalise(String email) {
        return email.trim().toLowerCase();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
