package com.anastasia.Anastasia_BackEnd.modules.calendar.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "calendar_occurrence_overrides", indexes = {
        @Index(name = "idx_calendar_override_entry_date", columnList = "entry_id, occurrence_date")
})
public class CalendarOccurrenceOverrideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private CalendarEntryEntity entry;

    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate;

    @Column(name = "is_cancelled", nullable = false)
    private boolean cancelled;

    @Column(name = "title_override")
    private String titleOverride;

    @Column(name = "start_at_utc_override")
    private Instant startAtUtcOverride;

    @Column(name = "end_at_utc_override")
    private Instant endAtUtcOverride;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
