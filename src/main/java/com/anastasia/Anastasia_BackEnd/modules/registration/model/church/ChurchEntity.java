package com.anastasia.Anastasia_BackEnd.modules.registration.model.church;

import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.common.LocalDateTimeAuditMetadata;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "churches",
        indexes = {
                @Index(name = "idx_churches_status", columnList = "status"),
                @Index(name = "idx_churches_uses_our_services", columnList = "uses_our_services")
        }
)
public class ChurchEntity extends LocalDateTimeAuditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "church_seq")
    @SequenceGenerator(name = "church_seq", sequenceName = "church_id_seq", allocationSize = 1)
    private Long churchId;

    @Column(unique = true, nullable = false)
    private String churchNumber;

    @OneToOne
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    @JsonIgnore
    private TenantEntity tenant;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ChurchStatus status = ChurchStatus.DRAFT;

    @Column(nullable = false)
    private String churchName;

    private String prefix;

    @Column(name = "prefix_local")
    private String prefixLocal;

    @Column(name = "church_name_local", nullable = false)
    private String churchNameLocal;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "avatar_id", referencedColumnName = "id")
    private ImageAssetEntity profilePicture;

    @Embedded
    private Address address;

    @Column(nullable = false)
    private String neighborhood;

    @Column(name = "neighborhood_local", nullable = false)
    private String neighborhoodLocal;

    @Column(nullable = false)
    private String diocese;

    @Column(name = "diocese_local", nullable = false)
    private String dioceseLocal;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(name = "timezone", nullable = false, length = 64)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "locale", nullable = false, length = 16)
    @Builder.Default
    private String locale = "en-US";

    private String denomination;

    @Column(length = 1000)
    private String description;

    @Column(name = "description_local", length = 1000)
    private String descriptionLocal;

    @Column(name = "uses_our_services", nullable = false)
    private boolean usesOurServices;

    private String gpsLocation;

    private Double latitude;

    private Double longitude;

    private String website;

    private String instagram;

    @Column(name = "youtube_page")
    private String youtube;

    @Column(name = "facebook_page")
    private String facebook;

    @Column(name = "is_church_profile_complete", nullable = false)
    private boolean churchProfileComplete;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "church", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<GroupEntity> groups;

    @OneToMany(mappedBy = "church", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<EventEntity> events;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.churchProfileComplete = isComplete();
        initializeAuditTimestamps(now);
        if (status == null) {
            status = ChurchStatus.DRAFT;
        }
        if (timezone == null || timezone.isBlank()) {
            timezone = "UTC";
        }
        if (locale == null || locale.isBlank()) {
            locale = "en-US";
        }
    }

    @PreUpdate
    void onUpdate() {
        this.churchProfileComplete = isComplete();
        touchAuditTimestamps(Instant.now());
    }

    public boolean isComplete() {
        return hasText(churchName)
                && hasText(churchNameLocal)
                && hasText(neighborhood)
                && hasText(neighborhoodLocal)
                && hasText(diocese)
                && hasText(dioceseLocal)
                && hasText(email)
                && hasText(phone)
                && hasText(description)
                && isHttpUrl(gpsLocation)
                && profilePicture != null
                && hasText(profilePicture.getImageUrl())
                && address != null
                && hasText(address.getAddressLine1())
                && hasText(address.getCity())
                && hasText(address.getCountry());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isHttpUrl(String value) {
        if (!hasText(value)) {
            return false;
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && hasText(host);
        } catch (Exception ex) {
            return false;
        }
    }
}
