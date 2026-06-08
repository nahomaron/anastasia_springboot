package com.anastasia.Anastasia_BackEnd.modules.events.model.attendance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateAttendanceStatusRequestDTO {
    private Long attendanceId;
    private Long eventId;
    private UUID userId;
    private String guestFullName;
    private String guestEmail;
    private String guestPhone;
    private AttendanceStatus status;
    private String checkInMethod;
    @JsonIgnore
    private UUID updatedBy;
}
