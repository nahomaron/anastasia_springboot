package com.anastasia.Anastasia_BackEnd.auditing;

import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    private String action; // e.g., "PASSWORD_CHANGE"

    private LocalDateTime timestamp = LocalDateTime.now();

    private String ipAddress;

    private String userAgent;
}
