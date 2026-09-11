package com.interview.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Business-level audit trail (distinct from the technical @CreatedDate/@LastModifiedDate
 * auditing on other entities). Written by AuditLogService using
 * Propagation.REQUIRES_NEW so that an audit record is durably committed EVEN IF the
 * enclosing business transaction later rolls back (e.g. a rejected trade should still
 * leave a permanent "REJECTED" audit trail for compliance).
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String entityName;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(nullable = false, length = 60)
    private String performedBy;

    @Column(length = 500)
    private String details;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
