package com.anastasia.Anastasia_BackEnd.model.event.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventManagerDTO {
    private Long eventId;
    private UUID userId;
    private String role;
    private LocalDateTime assignedAt;
}
