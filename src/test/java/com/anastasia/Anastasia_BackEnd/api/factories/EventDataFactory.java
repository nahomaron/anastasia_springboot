package com.anastasia.Anastasia_BackEnd.api.factories;

import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.event.EventDTO;
import com.anastasia.Anastasia_BackEnd.model.event.EventVisibilityType;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.CheckInQRRequestDTO;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.CheckInRequestDTO;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.MarkAbsentRequestDTO;
import com.anastasia.Anastasia_BackEnd.model.event.requests.AssignEventManagerRequest;
import io.restassured.http.ContentType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Factory for composing Event-related payloads.
 */
public final class EventDataFactory {

    private EventDataFactory() {
    }

    public static EventDTO newEvent(Long churchId) {
        ChurchEntity churchReference = null;
        if (churchId != null) {
            churchReference = new ChurchEntity();
            churchReference.setChurchId(churchId);
        }

        return EventDTO.builder()
                .church(churchReference)
                .title("Community Gathering " + System.currentTimeMillis())
                .description("Quarterly community gathering for families.")
                .date(LocalDate.now().plusDays(7))
                .location("Main Parish Hall")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .visibility(EventVisibilityType.ALL)
                .image("https://cdn.example.com/events/banner.png")
                .build();
    }

    public static AssignEventManagerRequest assignManagerRequest(UUID userId, String role) {
        return AssignEventManagerRequest.builder()
                .userId(userId)
                .role(role == null ? "COORDINATOR" : role)
                .build();
    }

    public static CheckInRequestDTO checkInRequest(Long eventId, UUID userId) {
        return CheckInRequestDTO.builder()
                .eventId(eventId)
                .userId(userId)
                .checkInMethod("MANUAL")
                .checkedInBy(userId)
                .build();
    }

    public static CheckInQRRequestDTO checkInQrRequest(Long eventId, UUID userId) {
        return CheckInQRRequestDTO.builder()
                .eventId(eventId)
                .userId(userId)
                .latitude(38.897957)
                .longitude(-77.03656)
                .build();
    }

    public static MarkAbsentRequestDTO markAbsentRequest(Long eventId, UUID userId) {
        return MarkAbsentRequestDTO.builder()
                .eventId(eventId)
                .userId(userId)
                .checkInMethod("MANUAL")
                .markedAbsentBy(userId)
                .build();
    }
}
