package com.adwitiya.feedbackportal.service;

import com.adwitiya.feedbackportal.config.properties.AppProperties;
import com.adwitiya.feedbackportal.domain.entity.Admin;
import com.adwitiya.feedbackportal.domain.entity.Department;
import com.adwitiya.feedbackportal.domain.entity.User;
import com.adwitiya.feedbackportal.domain.enums.Role;
import com.adwitiya.feedbackportal.repository.AdminRepository;
import com.adwitiya.feedbackportal.repository.DepartmentRepository;
import com.adwitiya.feedbackportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first super-administrator on an otherwise empty database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapRunner implements ApplicationRunner {

    private static final String DEFAULT_DEPARTMENT_CODE = "IT";

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Bootstrap config = appProperties.getBootstrap();
        if (!config.isEnabled()) {
            return;
        }
        if (userRepository.existsByRole(Role.SUPER_ADMIN)) {
            log.debug("A super-administrator already exists; skipping bootstrap");
            return;
        }
        if (config.getPassword() == null || config.getPassword().isBlank()) {
            log.warn("""
                    No super-administrator exists and BOOTSTRAP_ADMIN_PASSWORD is not set.
                    Set it and restart, or the portal cannot be administered.""");
            return;
        }

        Department department = departmentRepository.findByCodeIgnoreCase(DEFAULT_DEPARTMENT_CODE)
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .code(DEFAULT_DEPARTMENT_CODE)
                        .name("IT Services")
                        .description("Created automatically during bootstrap")
                        .active(true)
                        .build()));

        User user = userRepository.save(User.builder()
                .email(config.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(config.getPassword()))
                .fullName("Portal Administrator")
                .role(Role.SUPER_ADMIN)
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .version(0L)
                .build());

        adminRepository.save(Admin.builder()
                .user(user)
                .employeeCode("SUPERADMIN")
                .department(department)
                .designation("System Administrator")
                .build());

        log.info("Bootstrapped super-administrator '{}'. Change this password after first sign-in.",
                user.getEmail());
    }
}
