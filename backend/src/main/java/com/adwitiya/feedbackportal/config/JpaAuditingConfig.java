package com.adwitiya.feedbackportal.config;

import com.adwitiya.feedbackportal.security.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Enables {@code @CreatedDate} / {@code @LastModifiedDate} population and
 * exposes the acting user to Spring Data.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(SecurityUtils.currentUserEmail().orElse("system"));
    }
}
