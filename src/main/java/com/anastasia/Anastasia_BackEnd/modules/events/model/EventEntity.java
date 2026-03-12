package com.anastasia.Anastasia_BackEnd.modules.events.model;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_event_church", columnList = "church_id"),
        @Index(name = "idx_event_tenant", columnList = "tenantId"),
        @Index(name = "idx_event_start_at", columnList = "startAt")
})
@EntityListeners(AuditingEntityListener.class)
public class EventEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long eventId;

    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "church_id", nullable = false)
    private ChurchEntity church;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 512)
    private String location;

    @Column(length = 2048)
    private String gpsLocation;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Builder.Default
    @Column(nullable = false, length = 64)
    private String timezone = "UTC";

    @Builder.Default
    @Column(nullable = false)
    private boolean allDay = false;

    @Column(length = 2048)
    private String image;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 24)
    private EventStatus status = EventStatus.SCHEDULED;

    private LocalDateTime canceledAt;

    private LocalDateTime statusChangedAt;

    @Enumerated(EnumType.STRING)
    private EventType type;

    private Integer capacity;

    private Boolean requiresRegistration;

    private Boolean allowWaitlist;

    private Boolean allowGeoCheckIn;

    private Double latitude;

    private Double longitude;

    private Integer geofenceRadiusMeters;

    private LocalDateTime checkInOpensAt;

    private LocalDateTime checkInClosesAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "invited_groups",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<GroupEntity> invitedGroups;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "invited_users",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<UserEntity> invitedUsers;

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "event_invited_emails", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "email", nullable = false)
    private Set<String> invitedEmails = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private EventVisibilityType visibility;

    @Enumerated(EnumType.STRING)
    private Repetition repetition;

    @Builder.Default
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EventManagerEntity> eventManagers = new HashSet<>();

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    public void onCreate() {
        if (timezone == null || timezone.isBlank()) {
            timezone = "UTC";
        }
        if (status == null) {
            status = EventStatus.SCHEDULED;
        }
        if (statusChangedAt == null) {
            statusChangedAt = LocalDateTime.now();
        }
    }

    @Transient
    public Duration getDuration() {
        if (startAt == null || endAt == null) {
            return Duration.ZERO;
        }
        return Duration.between(startAt, endAt);
    }

}
