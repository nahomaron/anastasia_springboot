package com.anastasia.Anastasia_BackEnd.modules.appointments.repository;

import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppointmentStatusHistoryRepository extends JpaRepository<AppointmentStatusHistoryEntity, UUID> {
}
