package com.anastasia.Anastasia_BackEnd.modules.appointments.repository;

import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppointmentAssignmentRepository extends JpaRepository<AppointmentAssignmentEntity, UUID> {
}
