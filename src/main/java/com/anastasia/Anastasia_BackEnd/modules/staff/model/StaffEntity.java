package com.anastasia.Anastasia_BackEnd.modules.staff.model;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "staff", indexes = {
        @Index(name = "idx_staff_tenant", columnList = "tenant_id"),
        @Index(name = "idx_staff_church", columnList = "church_id"),
        @Index(name = "idx_staff_status", columnList = "employment_status")
})
public class StaffEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String staffNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "church_id", nullable = false)
    private ChurchEntity church;

    @Column(nullable = false, length = 64)
    private String churchNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "position_type", nullable = false, length = 64)
    private StaffPositionType positionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 32)
    private StaffEmploymentStatus employmentStatus;

    @Column(length = 128)
    private String department;

    @Column(name = "primary_phone", length = 64)
    private String primaryPhone;

    @Column(name = "alternate_phone", length = 64)
    private String alternatePhone;

    private LocalDate hireDate;

    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reports_to_staff_id")
    private StaffEntity reportsTo;

    @Column(length = 2000)
    private String notes;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "invite_accepted_at")
    private LocalDateTime inviteAcceptedAt;

    @Column(name = "first_login_at")
    private LocalDateTime firstLoginAt;

    @Column(name = "last_credential_reset_at")
    private LocalDateTime lastCredentialResetAt;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Version
    @Column(nullable = false)
    private long version;
}
