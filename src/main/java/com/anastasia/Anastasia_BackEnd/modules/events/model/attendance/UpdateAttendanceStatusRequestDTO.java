package com.anastasia.Anastasia_BackEnd.modules.events.model.attendance;

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
    private Long eventId;
    private UUID userId;
    private AttendanceStatus status;
    private String checkInMethod;
    private UUID updatedBy;
}
