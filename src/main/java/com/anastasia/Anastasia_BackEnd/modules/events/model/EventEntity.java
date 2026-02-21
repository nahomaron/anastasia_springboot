package com.anastasia.Anastasia_BackEnd.modules.events.model;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
        @Index(name = "idx_event_date", columnList = "date")
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

    @Lob
    private String description;

    private LocalDate date;

    private String location;

    private LocalTime startTime;

    private LocalTime endTime;

    private String image;

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

    @Enumerated(EnumType.STRING)
    private EventVisibilityType visibility;

    @Enumerated(EnumType.STRING)
    private Repetition repetition;

    @Builder.Default
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EventManagerEntity> eventManagers = new HashSet<>();



    @Transient
    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }

}
