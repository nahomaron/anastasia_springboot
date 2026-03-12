package com.anastasia.Anastasia_BackEnd.modules.staff.dto;

import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEmploymentStatus;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffPositionType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateStaffRequest(
        @NotBlank(message = "fullName is required")
        @Size(max = 255, message = "fullName must be at most 255 characters")
        String fullName,
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,
        String phoneNumber,
        Long churchId,
        @NotNull(message = "positionType is required")
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
