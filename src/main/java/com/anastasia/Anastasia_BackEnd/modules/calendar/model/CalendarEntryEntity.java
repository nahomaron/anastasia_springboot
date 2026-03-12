package com.anastasia.Anastasia_BackEnd.modules.calendar.model;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "calendar_entries", indexes = {
        @Index(name = "idx_calendar_entry_tenant_church_start", columnList = "tenant_id, church_id, start_at_utc"),
        @Index(name = "idx_calendar_entry_tenant_church_type", columnList = "tenant_id, church_id, type"),
        @Index(name = "idx_calendar_entry_tenant_church_visibility", columnList = "tenant_id, church_id, visibility")
})
@EntityListeners(AuditingEntityListener.class)
public class CalendarEntryEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "church_id", nullable = false)
    private ChurchEntity church;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private UserEntity ownerUser;

    @Column(name = "owner_user_id", insertable = false, updatable = false)
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CalendarEntryType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CalendarSystem calendarSystem;

    @Column(name = "start_at_utc", nullable = false)
    private Instant startAtUtc;

    @Column(name = "end_at_utc")
    private Instant endAtUtc;

    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private boolean allDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CalendarVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    @Builder.Default
    private CalendarEntryStatus status = CalendarEntryStatus.SCHEDULED;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_entity_type", length = 24)
    @Builder.Default
    private CalendarEntrySourceType sourceEntityType = CalendarEntrySourceType.MANUAL;

    @Column(name = "source_entity_id")
    private UUID sourceEntityId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "calendar_entry_categories", joinColumns = @JoinColumn(name = "entry_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    @Builder.Default
    private Set<CalendarCategory> categories = new HashSet<>();

    @OneToOne(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private CalendarRecurrenceEntity recurrence;

    @Builder.Default
    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CalendarOccurrenceOverrideEntity> overrides = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CalendarEntryAudienceEntity> audiences = new HashSet<>();

    @Version
    @Column(nullable = false)
    private long version;
}
