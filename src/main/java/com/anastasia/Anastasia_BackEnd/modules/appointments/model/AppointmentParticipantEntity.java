package com.anastasia.Anastasia_BackEnd.modules.appointments.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "appointment_participants", indexes = {
        @Index(name = "idx_appointment_participant_appointment", columnList = "appointment_id"),
        @Index(name = "idx_appointment_participant_member", columnList = "member_id")
})
public class AppointmentParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private AppointmentEntity appointment;

    @Column(name = "member_id")
    private Long memberId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private boolean familyMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantRole role;
}
