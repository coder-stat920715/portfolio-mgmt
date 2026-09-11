package com.interview.portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * @EnableJpaAuditing activates the AuditingEntityListener callbacks referenced on
 * Auditable (@CreatedDate/@LastModifiedDate). auditorAwareRef additionally allows
 * populating @CreatedBy/@LastModifiedBy fields if such fields are added later, by
 * resolving the "current auditor" (e.g. authenticated principal) on each write.
 *
 * Spring Security is intentionally NOT a dependency of this reference project, so the
 * AuditorAware bean below is stubbed to always return "SYSTEM". In a real deployment,
 * swap this implementation to read from SecurityContextHolder.getContext().getAuthentication().
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("SYSTEM");
    }
}
