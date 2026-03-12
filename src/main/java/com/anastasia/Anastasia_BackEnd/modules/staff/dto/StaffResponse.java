package com.anastasia.Anastasia_BackEnd.modules.staff.dto;

import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEmploymentStatus;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffPositionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record StaffResponse(
        Long id,
        String staffNumber,
        UUID userId,
        String fullName,
        String email,
        String phoneNumber,
        Long churchId,
        String churchNumber,
        String churchName,
        StaffPositionType positionType,
        StaffEmploymentStatus employmentStatus,
        String department,
        String primaryPhone,
        String alternatePhone,
        LocalDate hireDate,
        LocalDate endDate,
        Long reportsToStaffId,
        String reportsToStaffName,
        String notes,
        boolean mustChangePassword,
        LocalDateTime invitedAt,
        LocalDateTime inviteAcceptedAt,
        LocalDateTime firstLoginAt,
        LocalDateTime lastCredentialResetAt,
        LocalDateTime deactivatedAt,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate
) {
}
