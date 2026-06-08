package com.anastasia.Anastasia_BackEnd.modules.events.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.AttendanceStatus;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.AttendanceAttendeeType;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.CheckInRequestDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendance;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendanceResponse;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.MarkAbsentRequestDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.MemberAttendanceReportResponse;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.UpdateAttendanceStatusRequestDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventAttendanceRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventAttendanceService {

    private final UserRepository userRepository;
    private final EventAttendanceRepository attendanceRepository;
    private final EventRepository eventRepository;
    private final LocalizedMessageService messageService;

    public EventAttendance checkIn(CheckInRequestDTO requestDTO, UUID actorUserId) {
        UUID tenantId = requireTenantId();
        EventEntity event = findEventForTenant(requestDTO.getEventId(), tenantId);
        AttendanceTarget target = resolveTarget(tenantId, requestDTO.getUserId(), requestDTO.getGuestFullName(),
                requestDTO.getGuestEmail(), requestDTO.getGuestPhone());

        Optional<EventAttendance> existingAttendance = findExistingAttendance(event, target);

        if (existingAttendance.isPresent()) {
            throw new IllegalStateException(messageService.get(
                    "events.attendance.alreadyCheckedIn",
                    "Attendance already recorded"
            ));
        }

        EventAttendance attendance = EventAttendance.builder()
                .event(event)
                .user(target.user())
                .guestFullName(target.guestFullName())
                .guestEmail(target.guestEmail())
                .guestPhone(target.guestPhone())
                .checkInTime(LocalDateTime.now())
                .checkInMethod(requestDTO.getCheckInMethod())
                .status(AttendanceStatus.CHECKED_IN)
                .checkedInBy(actorUserId)
                .build();

        return attendanceRepository.save(attendance);
    }

    public EventAttendance markAbsent(MarkAbsentRequestDTO request, UUID actorUserId) {
        UUID tenantId = requireTenantId();
        EventEntity event = findEventForTenant(request.getEventId(), tenantId);
        AttendanceTarget target = resolveTarget(tenantId, request.getUserId(), request.getGuestFullName(),
                request.getGuestEmail(), request.getGuestPhone());

        Optional<EventAttendance> existingAttendance = findExistingAttendance(event, target);

        if (existingAttendance.isPresent()) {
            throw new IllegalStateException(messageService.get(
                    "events.attendance.alreadyRecorded",
                    "Attendance already recorded"
            ));
        }

        EventAttendance attendance = EventAttendance.builder()
                .event(event)
                .user(target.user())
                .guestFullName(target.guestFullName())
                .guestEmail(target.guestEmail())
                .guestPhone(target.guestPhone())
                .status(AttendanceStatus.ABSENT)
                .checkInMethod(request.getCheckInMethod())
                .checkedInBy(actorUserId)
                .build();

        return attendanceRepository.save(attendance);
    }

    public List<EventAttendance> getAttendanceByEvent(Long eventId) {
        UUID tenantId = requireTenantId();
        findEventForTenant(eventId, tenantId);
        return attendanceRepository.findByEventIdAndTenantId(eventId, tenantId);
    }

    public List<EventAttendance> getAttendanceByUser(UUID userId) {
        UUID tenantId = requireTenantId();
        findUserForTenant(userId, tenantId);
        return attendanceRepository.findByUserUuidAndTenantId(userId, tenantId);
    }

    public List<MemberAttendanceReportResponse> getAttendanceReportByUser(UUID userId) {
        UUID tenantId = requireTenantId();
        UserEntity user = findUserForTenant(userId, tenantId);
        ensureMemberLinkedUser(user);
        return attendanceRepository.findDetailedByUserUuidAndTenantId(userId, tenantId).stream()
                .map(attendance -> MemberAttendanceReportResponse.builder()
                        .id(attendance.getId())
                        .eventId(attendance.getEventId())
                        .eventTitle(attendance.getEvent().getTitle())
                        .eventType(attendance.getEvent().getType())
                        .eventStartAt(attendance.getEvent().getStartAt())
                        .eventTimezone(attendance.getEvent().getTimezone())
                        .location(attendance.getEvent().getLocation())
                        .checkInTime(attendance.getCheckInTime())
                        .checkInMethod(attendance.getCheckInMethod())
                        .status(attendance.getStatus())
                        .build())
                .toList();
    }

    public List<EventAttendance> getAttendanceByEventAndStatus(Long eventId, AttendanceStatus status) {
        UUID tenantId = requireTenantId();
        findEventForTenant(eventId, tenantId);
        return attendanceRepository.findByEventIdAndStatusAndTenantId(eventId, status, tenantId);
    }

    public List<EventAttendance> getAttendanceByUserAndStatus(UUID userId, AttendanceStatus status) {
        UUID tenantId = requireTenantId();
        findUserForTenant(userId, tenantId);
        return attendanceRepository.findByUserUuidAndStatusAndTenantId(userId, status, tenantId);
    }

    public EventAttendance updateAttendanceStatus(UpdateAttendanceStatusRequestDTO request, UUID actorUserId) {
        UUID tenantId = requireTenantId();
        EventAttendance attendance = resolveAttendanceForUpdate(request, tenantId);

        attendance.setStatus(request.getStatus());
        attendance.setCheckInMethod(request.getCheckInMethod());
        attendance.setCheckedInBy(actorUserId);
        if (request.getStatus() == AttendanceStatus.CHECKED_IN || request.getStatus() == AttendanceStatus.LATE) {
            attendance.setCheckInTime(LocalDateTime.now());
        }

        return attendanceRepository.save(attendance);
    }

    public EventAttendanceResponse toResponse(EventAttendance attendance) {
        return EventAttendanceResponse.builder()
                .id(attendance.getId())
                .eventId(attendance.getEventId())
                .userId(attendance.getUserId())
                .attendeeType(resolveAttendeeType(attendance))
                .attendeeName(resolveAttendeeName(attendance))
                .guestEmail(attendance.getGuestEmail())
                .guestPhone(attendance.getGuestPhone())
                .checkInTime(attendance.getCheckInTime())
                .checkInMethod(attendance.getCheckInMethod())
                .status(attendance.getStatus())
                .checkedInBy(attendance.getCheckedInBy())
                .build();
    }

    private EventEntity findEventForTenant(Long eventId, UUID tenantId) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "events.notFound",
                        "Event not found"
                )));
        if (!tenantId.equals(event.getTenantId())) {
            throw new EntityNotFoundException(messageService.get(
                    "events.notFound",
                    "Event not found"
            ));
        }
        return event;
    }

    private UserEntity findUserForTenant(UUID userId, UUID tenantId) {
        return userRepository.findByUuidAndAffiliatedTenantId(userId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "user.notFound",
                        "User not found"
                )));
    }

    private void ensureMemberLinkedUser(UserEntity user) {
        if (user == null || user.getMembership() == null || user.getMembership().getId() == null) {
            throw new IllegalArgumentException(messageService.get(
                    "events.attendance.memberReport.membershipRequired",
                    "Individual attendance reports require a member-linked user"
            ));
        }
    }

    private AttendanceTarget resolveTarget(
            UUID tenantId,
            UUID userId,
            String guestFullName,
            String guestEmail,
            String guestPhone
    ) {
        if (userId != null) {
            return AttendanceTarget.forUser(findUserForTenant(userId, tenantId));
        }

        String normalizedGuestName = normalizeGuestValue(guestFullName);
        String normalizedGuestEmail = normalizeGuestValue(guestEmail);
        String normalizedGuestPhone = normalizeGuestValue(guestPhone);
        if (normalizedGuestName == null) {
            throw new IllegalArgumentException(messageService.get(
                    "events.attendance.guest.nameRequired",
                    "guestFullName is required when userId is not provided"
            ));
        }
        return AttendanceTarget.forGuest(normalizedGuestName, normalizedGuestEmail, normalizedGuestPhone);
    }

    private Optional<EventAttendance> findExistingAttendance(EventEntity event, AttendanceTarget target) {
        if (target.user() != null) {
            return attendanceRepository.findByUserUuidAndEventId(target.user().getUuid(), event.getEventId());
        }

        return attendanceRepository.findByEventIdAndTenantId(event.getEventId(), event.getTenantId()).stream()
                .filter(existing -> existing.getUserId() == null)
                .filter(existing -> guestMatches(existing, target))
                .findFirst();
    }

    private EventAttendance resolveAttendanceForUpdate(UpdateAttendanceStatusRequestDTO request, UUID tenantId) {
        if (request.getAttendanceId() != null) {
            return attendanceRepository.findByIdAndEventTenantId(request.getAttendanceId(), tenantId)
                    .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                            "events.attendance.notFound",
                            "Attendance not found"
                    )));
        }

        EventEntity event = findEventForTenant(request.getEventId(), tenantId);
        AttendanceTarget target = resolveTarget(tenantId, request.getUserId(), request.getGuestFullName(),
                request.getGuestEmail(), request.getGuestPhone());

        return findExistingAttendance(event, target)
                .orElseGet(() -> EventAttendance.builder()
                        .event(event)
                        .user(target.user())
                        .guestFullName(target.guestFullName())
                        .guestEmail(target.guestEmail())
                        .guestPhone(target.guestPhone())
                        .build());
    }

    private boolean guestMatches(EventAttendance existing, AttendanceTarget target) {
        String existingEmail = normalizeGuestValue(existing.getGuestEmail());
        String existingPhone = normalizeGuestValue(existing.getGuestPhone());
        String existingName = normalizeGuestValue(existing.getGuestFullName());

        if (target.guestEmail() != null && existingEmail != null) {
            return target.guestEmail().equalsIgnoreCase(existingEmail);
        }
        if (target.guestPhone() != null && existingPhone != null) {
            return target.guestPhone().equals(existingPhone);
        }
        return target.guestFullName() != null
                && existingName != null
                && target.guestFullName().equalsIgnoreCase(existingName);
    }

    private AttendanceAttendeeType resolveAttendeeType(EventAttendance attendance) {
        if (attendance.getUser() == null) {
            return AttendanceAttendeeType.GUEST;
        }
        UserType userType = attendance.getUser().getUserType();
        if (userType == null) {
            return AttendanceAttendeeType.USER;
        }
        return switch (userType) {
            case MEMBER -> AttendanceAttendeeType.MEMBER;
            case STAFF -> AttendanceAttendeeType.STAFF;
            case PRIEST -> AttendanceAttendeeType.PRIEST;
            case GUEST -> AttendanceAttendeeType.GUEST;
            default -> AttendanceAttendeeType.USER;
        };
    }

    private String resolveAttendeeName(EventAttendance attendance) {
        if (attendance.getUser() != null && attendance.getUser().getFullName() != null
                && !attendance.getUser().getFullName().isBlank()) {
            return attendance.getUser().getFullName();
        }
        return attendance.getGuestFullName();
    }

    private String normalizeGuestValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException(messageService.get(
                    "tenant.context.missing",
                    "Tenant ID not found in context"
            ));
        }
        return tenantId;
    }

    private record AttendanceTarget(UserEntity user, String guestFullName, String guestEmail, String guestPhone) {
        private static AttendanceTarget forUser(UserEntity user) {
            return new AttendanceTarget(user, null, null, null);
        }

        private static AttendanceTarget forGuest(String guestFullName, String guestEmail, String guestPhone) {
            return new AttendanceTarget(null, guestFullName, guestEmail, guestPhone);
        }
    }
}
