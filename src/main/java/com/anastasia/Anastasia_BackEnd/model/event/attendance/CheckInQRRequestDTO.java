package com.anastasia.Anastasia_BackEnd.model.event.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CheckInQRRequestDTO {

    private Long eventId;

    private UUID userId;

    private double longitude;

    private double latitude;
}
