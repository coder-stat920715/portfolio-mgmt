package com.interview.portfolio.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Interview concept: @MappedSuperclass shares mapping metadata (columns) with subclasses
 * WITHOUT its own table and WITHOUT being an entity itself - contrast this with
 * @Inheritance strategies (SINGLE_TABLE / JOINED / TABLE_PER_CLASS) used on Asset below,
 * which DO produce entities with their own identity/table semantics.
 *
 * @EntityListeners(AuditingEntityListener.class) hooks into JPA lifecycle callbacks
 * (@PrePersist / @PreUpdate) to auto-populate @CreatedDate / @LastModifiedDate.
 * Requires @EnableJpaAuditing on a @Configuration class (see JpaAuditingConfig).
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}
