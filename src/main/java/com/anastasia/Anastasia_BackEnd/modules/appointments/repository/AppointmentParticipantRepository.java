package com.anastasia.Anastasia_BackEnd.modules.appointments.repository;

import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppointmentParticipantRepository extends JpaRepository<AppointmentParticipantEntity, UUID> {
}
