package com.anastasia.Anastasia_BackEnd.common.auditing;

import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @SequenceGenerator(name = "audit_logs_seq_gen", sequenceName = "audit_logs_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_logs_seq_gen")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(nullable = false, length = 96)
    private String action; // Normalized event name from AuditEventType.

    @Column(name = "actor_identifier", length = 191)
    private String actorIdentifier;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "target_type", length = 64)
    private String targetType;

    @Column(name = "target_id", length = 128)
    private String targetId;

    @Column(name = "result", length = 32)
    private String result;

    @Column(name = "reason", length = 512)
    private String reason;

    @Column(name = "context", length = 2000)
    private String context;

    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(length = 96)
    private String ipAddress;

    @Column(length = 512)
    private String userAgent;
}
