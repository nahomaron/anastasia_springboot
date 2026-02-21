package com.anastasia.Anastasia_BackEnd.modules.appointments.model;

import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryEntity;
import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Builder
@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appointment_tenant_church_start", columnList = "tenant_id, church_id, start_at_utc"),
        @Index(name = "idx_appointment_tenant_church_status", columnList = "tenant_id, church_id, status"),
        @Index(name = "idx_appointment_tenant_church_type", columnList = "tenant_id, church_id, type")
})
@EntityListeners(AuditingEntityListener.class)
public class AppointmentEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "church_id", nullable = false)
    private ChurchEntity church;

    @Column(name = "church_id", insertable = false, updatable = false)
    private Long churchId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calendar_entry_id")
    private CalendarEntryEntity calendarEntry;

    @Column(name = "calendar_entry_id", insertable = false, updatable = false)
    private UUID calendarEntryId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType locationType;

    @Column(nullable = false)
    private String locationLabel;

    @Column(name = "start_at_utc", nullable = false)
    private Instant startAtUtc;

    @Column(name = "end_at_utc")
    private Instant endAtUtc;

    @Column(nullable = false)
    private String timezone;

    @Column(columnDefinition = "TEXT")
    private String notesForMember;

    @Column(nullable = false)
    private boolean privateNotesExists;

    private String contactInfo;

    private UUID linkedRequestId;

    @Column(nullable = false)
    private boolean firstVisit;

    @Column(nullable = false)
    private boolean sacramentRelated;

    @Builder.Default
    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<AppointmentParticipantEntity> participants = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<AppointmentAssignmentEntity> assignments = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<AppointmentStatusHistoryEntity> statusHistory = new HashSet<>();
}
