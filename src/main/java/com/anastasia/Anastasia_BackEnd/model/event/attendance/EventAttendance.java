package com.anastasia.Anastasia_BackEnd.model.event.attendance;

import com.anastasia.Anastasia_BackEnd.model.event.EventEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class EventAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @Column(name = "event_id", insertable = false, updatable = false)
    private Long eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    private LocalDateTime checkInTime;

    private String checkInMethod; // e.g. "QR", "Manual"

    @Enumerated(EnumType.STRING)
    private AttendanceStatus status; // e.g. CHECKED_IN, ABSENT, etc.

    private UUID checkedInBy; // Optional: admin ID
}
