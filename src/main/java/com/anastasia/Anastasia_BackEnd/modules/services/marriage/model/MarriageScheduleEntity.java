package com.anastasia.Anastasia_BackEnd.modules.services.marriage.model;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "marriage_schedules", indexes = {
        @Index(name = "idx_marriage_schedule_case_status", columnList = "marriage_case_id, schedule_status"),
        @Index(name = "idx_marriage_schedule_priest", columnList = "assigned_priest_user_id, approved_date_time")
})
public class MarriageScheduleEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marriage_case_id", nullable = false)
    private MarriageCaseEntity marriageCase;

    @Column(name = "proposed_date_time")
    private Instant proposedDateTime;

    @Column(name = "approved_date_time")
    private Instant approvedDateTime;

    @Column(name = "place_label", length = 255)
    private String placeLabel;

    @Column(name = "admin_calendar_event_id")
    private UUID adminCalendarEventId;

    @Column(name = "priest_calendar_event_id")
    private UUID priestCalendarEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_status", nullable = false, length = 24)
    @Builder.Default
    private MarriageScheduleStatus scheduleStatus = MarriageScheduleStatus.DRAFT;

    @Column(name = "reschedule_count", nullable = false)
    @Builder.Default
    private int rescheduleCount = 0;

    @Column(name = "assigned_priest_user_id")
    private UUID assignedPriestUserId;

    @Column(name = "scheduling_note", length = 2000)
    private String schedulingNote;

    @Version
    @Column(nullable = false)
    private long version;
}
