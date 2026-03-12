package com.anastasia.Anastasia_BackEnd.modules.staff.dto;

import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEmploymentStatus;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffPositionType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateStaffRequest(
        @Size(max = 255, message = "fullName must be at most 255 characters")
        String fullName,
        @Email(message = "email must be valid")
        String email,
        String phoneNumber,
        Long churchId,
        StaffPositionType positionType,
        StaffEmploymentStatus employmentStatus,
        String department,
        String primaryPhone,
        String alternatePhone,
        LocalDate hireDate,
        LocalDate endDate,
        Long reportsToStaffId,
        String notes
) {
}
