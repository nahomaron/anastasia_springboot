package com.anastasia.Anastasia_BackEnd.modules.platform.admin.model;

import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformAnnouncementChannel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "platform_settings")
public class PlatformAdminSettingsEntity {

    @Id
    private UUID id;

    @Column(name = "maintenance_mode", nullable = false)
    private boolean maintenanceMode;

    @Column(name = "auto_renewal_interval", nullable = false, length = 32)
    private String autoRenewalInterval;

    @Column(name = "support_hours", length = 128)
    private String supportHours;

    @Column(name = "customer_success_email", length = 160)
    private String customerSuccessEmail;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "platform_settings_channels", joinColumns = @JoinColumn(name = "platform_settings_id"))
    @Column(name = "channel", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    @Fetch(FetchMode.SUBSELECT)
    private Set<PlatformAnnouncementChannel> announcementChannels = new LinkedHashSet<>();

    @Column(name = "enable_auto_assign_priests", nullable = false)
    private boolean enableAutoAssignPriests;

    @Column(name = "enable_manual_plan_overrides", nullable = false)
    private boolean enableManualPlanOverrides;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }
}
