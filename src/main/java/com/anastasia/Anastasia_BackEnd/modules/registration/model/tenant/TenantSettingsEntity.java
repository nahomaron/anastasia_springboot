package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tenant_settings")
public class TenantSettingsEntity {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Builder.Default
    @Column(name = "attendance_kiosk_mode_enabled", nullable = false)
    private boolean attendanceKioskModeEnabled = false;

    @Builder.Default
    @Column(name = "attendance_newcomer_capture_enabled", nullable = false)
    private boolean attendanceNewcomerCaptureEnabled = true;

    @Builder.Default
    @Column(name = "attendance_capture_full_name", nullable = false)
    private boolean attendanceCaptureFullName = true;

    @Builder.Default
    @Column(name = "attendance_capture_email", nullable = false)
    private boolean attendanceCaptureEmail = true;

    @Builder.Default
    @Column(name = "attendance_capture_phone", nullable = false)
    private boolean attendanceCapturePhone = false;

    @Builder.Default
    @Column(name = "email_quota_enforced", nullable = false)
    private boolean emailQuotaEnforced = true;

    @Builder.Default
    @Column(name = "email_sending_suspended", nullable = false)
    private boolean emailSendingSuspended = false;

    @Column(name = "email_monthly_quota")
    private Integer emailMonthlyQuota;

    @Column(name = "email_suspension_reason", length = 512)
    private String emailSuspensionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
