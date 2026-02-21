package com.anastasia.Anastasia_BackEnd.modules.appointments.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentAssigneeRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentCreateRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentParticipantRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentRescheduleRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentResponse;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentStatusUpdateRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.mappers.AppointmentMapper;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentParticipantEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatus;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatusHistoryEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentType;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.ParticipantRole;
import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.CalendarEntryRequest;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarCategory;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryType;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarSystem;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarVisibility;
import com.anastasia.Anastasia_BackEnd.modules.calendar.service.CalendarEntryService;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.appointments.repository.AppointmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.BaseMember;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final CalendarEntryService calendarEntryService;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final ChildRepository childRepository;

    @Override
    @Transactional
    public AppointmentResponse createAppointment(AppointmentCreateRequest request, UUID userId) {
        validateRequest(request);
        UUID tenantId = requireTenantId();
        ChurchEntity church = resolveChurch(tenantId);

        AppointmentStatus status = Optional.ofNullable(request.status())
                .orElse(AppointmentStatus.REQUESTED);

        CalendarEntryEntity calendarEntry = createCalendarEntry(request, userId);

        AppointmentEntity appointment = AppointmentEntity.builder()
                .tenantId(tenantId)
                .church(church)
                .calendarEntry(calendarEntry)
                .title(request.title())
                .description(request.description())
                .type(request.type())
                .status(status)
                .source(Optional.ofNullable(request.source()).orElse(com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentSource.MANUAL))
                .locationType(request.locationType())
                .locationLabel(request.locationLabel())
                .startAtUtc(request.startDateTime())
                .endAtUtc(request.endDateTime())
                .timezone(request.timeZone())
                .notesForMember(request.notesForMember())
                .privateNotesExists(false)
                .contactInfo(request.contactInfo())
                .linkedRequestId(request.linkedRequestId())
                .firstVisit(resolveFirstVisit(tenantId, request.participants()))
                .sacramentRelated(isSacramentRelated(request.type()))
                .build();

        Set<AppointmentParticipantEntity> participants = buildParticipants(request.participants(), appointment);
        Set<AppointmentAssignmentEntity> assignments = buildAssignments(request.assignees(), appointment);

        ensureNoConflicts(tenantId, assignments, request.startDateTime(), request.endDateTime(), null);

        appointment.getParticipants().addAll(participants);
        appointment.getAssignments().addAll(assignments);
        appointment.getStatusHistory().add(buildStatusHistory(appointment, null, status, "Created", userId));

        AppointmentEntity saved = appointmentRepository.save(appointment);
        enrichNames(saved);
        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(UUID appointmentId) {
        AppointmentEntity appointment = resolveAppointment(appointmentId);
        enrichNames(appointment);
        return appointmentMapper.toResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> listAppointments(Instant start, Instant end, AppointmentStatus status, AppointmentType type) {
        UUID tenantId = requireTenantId();

        List<AppointmentEntity> appointments;
        if (start != null && end != null) {
            appointments = appointmentRepository.findForRange(tenantId, start, end);
        } else if (status != null) {
            appointments = appointmentRepository.findByTenantIdAndStatus(tenantId, status);
        } else if (type != null) {
            appointments = appointmentRepository.findByTenantIdAndType(tenantId, type);
        } else {
            appointments = appointmentRepository.findByTenantId(tenantId);
        }

        enrichNames(appointments);
        return appointments.stream()
                .map(appointmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AppointmentResponse rescheduleAppointment(UUID appointmentId, AppointmentRescheduleRequest request, UUID userId) {
        validateReschedule(request);
        AppointmentEntity appointment = resolveAppointment(appointmentId);
        AppointmentStatus previous = appointment.getStatus();

        ensureNoConflicts(
                appointment.getTenantId(),
                appointment.getAssignments(),
                request.newStart(),
                request.newEnd(),
                appointment.getId()
        );

        appointment.setStartAtUtc(request.newStart());
        appointment.setEndAtUtc(request.newEnd());
        appointment.setStatus(AppointmentStatus.RESCHEDULED);

        if (request.reason() != null && !request.reason().isBlank()) {
            String reason = "Rescheduled: " + request.reason().trim();
            String existing = appointment.getNotesForMember();
            appointment.setNotesForMember(existing == null || existing.isBlank()
                    ? reason
                    : existing + "\n" + reason);
        }

        updateCalendarEntry(appointment, request.newStart(), request.newEnd(), userId);
        appointment.getStatusHistory().add(buildStatusHistory(
                appointment,
                previous,
                AppointmentStatus.RESCHEDULED,
                request.reason(),
                userId
        ));

        AppointmentEntity saved = appointmentRepository.save(appointment);
        enrichNames(saved);
        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse updateStatus(UUID appointmentId, AppointmentStatusUpdateRequest request, UUID userId) {
        AppointmentEntity appointment = resolveAppointment(appointmentId);
        AppointmentStatus previous = appointment.getStatus();

        appointment.setStatus(request.status());
        appointment.getStatusHistory().add(buildStatusHistory(
                appointment,
                previous,
                request.status(),
                request.reason(),
                userId
        ));

        AppointmentEntity saved = appointmentRepository.save(appointment);
        enrichNames(saved);
        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse addAssignees(UUID appointmentId, List<AppointmentAssigneeRequest> assignees, UUID userId) {
        AppointmentEntity appointment = resolveAppointment(appointmentId);
        if (assignees == null || assignees.isEmpty()) {
            return appointmentMapper.toResponse(appointment);
        }

        Set<AppointmentAssignmentEntity> newAssignments = new HashSet<>();
        for (AppointmentAssigneeRequest request : assignees) {
            boolean exists = appointment.getAssignments().stream()
                    .anyMatch(existing -> existing.getUserId().equals(request.userId()));
            if (!exists) {
                newAssignments.add(AppointmentAssignmentEntity.builder()
                        .appointment(appointment)
                        .userId(request.userId())
                        .role(request.role())
                        .build());
            }
        }

        ensureNoConflicts(
                appointment.getTenantId(),
                newAssignments,
                appointment.getStartAtUtc(),
                appointment.getEndAtUtc(),
                appointment.getId()
        );

        appointment.getAssignments().addAll(newAssignments);
        AppointmentEntity saved = appointmentRepository.save(appointment);
        enrichNames(saved);
        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse removeAssignee(UUID appointmentId, UUID userIdToRemove) {
        AppointmentEntity appointment = resolveAppointment(appointmentId);
        boolean removed = appointment.getAssignments().removeIf(assignment -> assignment.getUserId().equals(userIdToRemove));
        if (!removed) {
            throw new EntityNotFoundException("Assignee not found on appointment");
        }
        AppointmentEntity saved = appointmentRepository.save(appointment);
        enrichNames(saved);
        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse addParticipants(UUID appointmentId, List<AppointmentParticipantRequest> participants) {
        AppointmentEntity appointment = resolveAppointment(appointmentId);
        if (participants == null || participants.isEmpty()) {
            return appointmentMapper.toResponse(appointment);
        }

        for (AppointmentParticipantRequest request : participants) {
            boolean exists = appointment.getParticipants().stream()
                    .anyMatch(existing -> {
                        if (request.memberId() != null && existing.getMemberId() != null) {
                            return existing.getMemberId().equals(request.memberId());
                        }
                        return existing.getFullName().equalsIgnoreCase(request.fullName())
                                && existing.getRole() == request.role();
                    });
            if (!exists) {
                appointment.getParticipants().add(AppointmentParticipantEntity.builder()
                        .appointment(appointment)
                        .memberId(request.memberId())
                        .fullName(request.fullName())
                        .familyMember(request.familyMember())
                        .role(request.role())
                        .build());
            }
        }

        AppointmentEntity saved = appointmentRepository.save(appointment);
        enrichNames(saved);
        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse removeParticipant(UUID appointmentId, UUID participantId) {
        AppointmentEntity appointment = resolveAppointment(appointmentId);
        boolean removed = appointment.getParticipants().removeIf(participant -> participant.getId().equals(participantId));
        if (!removed) {
            throw new EntityNotFoundException("Participant not found on appointment");
        }
        AppointmentEntity saved = appointmentRepository.save(appointment);
        enrichNames(saved);
        return appointmentMapper.toResponse(saved);
    }

    private AppointmentEntity resolveAppointment(UUID appointmentId) {
        UUID tenantId = requireTenantId();
        return appointmentRepository.findByIdAndTenantId(appointmentId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Appointment not found"));
    }

    private CalendarEntryEntity createCalendarEntry(AppointmentCreateRequest request, UUID userId) {
        CalendarEntryRequest calendarRequest = new CalendarEntryRequest(
                CalendarEntryType.APPOINTMENT,
                request.title(),
                request.description(),
                CalendarSystem.GREGORIAN,
                request.startDateTime(),
                request.endDateTime(),
                request.timeZone(),
                false,
                Optional.ofNullable(request.visibility()).orElse(CalendarVisibility.PRIEST_ONLY),
                Set.of(CalendarCategory.APPOINTMENTS),
                null,
                null,
                null
        );
        return calendarEntryService.createEntry(calendarRequest, userId);
    }

    private void updateCalendarEntry(AppointmentEntity appointment, Instant start, Instant end, UUID userId) {
        CalendarEntryEntity entry = appointment.getCalendarEntry();
        if (entry == null) {
            return;
        }
        CalendarEntryRequest calendarRequest = new CalendarEntryRequest(
                entry.getType(),
                entry.getTitle(),
                entry.getDescription(),
                entry.getCalendarSystem(),
                start,
                end,
                entry.getTimezone(),
                entry.isAllDay(),
                entry.getVisibility(),
                entry.getCategories(),
                null,
                null,
                null
        );
        calendarEntryService.updateEntry(entry.getId(), calendarRequest, userId);
    }

    private Set<AppointmentParticipantEntity> buildParticipants(
            Set<AppointmentParticipantRequest> requests,
            AppointmentEntity appointment
    ) {
        if (requests == null || requests.isEmpty()) {
            return Set.of();
        }
        Set<AppointmentParticipantEntity> participants = new HashSet<>();
        for (AppointmentParticipantRequest request : requests) {
            AppointmentParticipantEntity participant = AppointmentParticipantEntity.builder()
                    .appointment(appointment)
                    .memberId(request.memberId())
                    .fullName(request.fullName())
                    .familyMember(request.familyMember())
                    .role(request.role())
                    .build();
            participants.add(participant);
        }
        return participants;
    }

    private Set<AppointmentAssignmentEntity> buildAssignments(
            Set<AppointmentAssigneeRequest> requests,
            AppointmentEntity appointment
    ) {
        if (requests == null || requests.isEmpty()) {
            return Set.of();
        }
        Set<AppointmentAssignmentEntity> assignments = new HashSet<>();
        for (AppointmentAssigneeRequest request : requests) {
            AppointmentAssignmentEntity assignment = AppointmentAssignmentEntity.builder()
                    .appointment(appointment)
                    .userId(request.userId())
                    .role(request.role())
                    .build();
            assignments.add(assignment);
        }
        return assignments;
    }

    private AppointmentStatusHistoryEntity buildStatusHistory(
            AppointmentEntity appointment,
            AppointmentStatus from,
            AppointmentStatus to,
            String reason,
            UUID userId
    ) {
        return AppointmentStatusHistoryEntity.builder()
                .appointment(appointment)
                .fromStatus(from)
                .toStatus(to)
                .reason(reason)
                .changedByUserId(userId)
                .build();
    }

    private boolean resolveFirstVisit(UUID tenantId, Set<AppointmentParticipantRequest> participants) {
        if (participants == null) {
            return false;
        }
        return participants.stream()
                .filter(p -> p.role() == ParticipantRole.MEMBER)
                .map(AppointmentParticipantRequest::memberId)
                .filter(memberId -> memberId != null)
                .findFirst()
                .map(memberId -> appointmentRepository.countByTenantIdAndMemberId(tenantId, memberId) == 0)
                .orElse(false);
    }

    private boolean isSacramentRelated(AppointmentType type) {
        return type == AppointmentType.CONFESSION
                || type == AppointmentType.PRE_MARITAL
                || type == AppointmentType.BAPTISM_PREP;
    }

    private void enrichNames(AppointmentEntity appointment) {
        if (appointment == null) {
            return;
        }
        enrichNames(List.of(appointment));
    }

    private void enrichNames(List<AppointmentEntity> appointments) {
        if (appointments == null || appointments.isEmpty()) {
            return;
        }

        Set<UUID> userIds = new HashSet<>();
        Set<Long> memberIds = new HashSet<>();

        for (AppointmentEntity appointment : appointments) {
            if (appointment.getAssignments() != null) {
                for (AppointmentAssignmentEntity assignment : appointment.getAssignments()) {
                    if (assignment.getUserId() != null) {
                        userIds.add(assignment.getUserId());
                    }
                }
            }
            if (appointment.getParticipants() != null) {
                for (AppointmentParticipantEntity participant : appointment.getParticipants()) {
                    if (participant.getMemberId() != null) {
                        memberIds.add(participant.getMemberId());
                    }
                }
            }
        }

        Map<UUID, String> userNames = new HashMap<>();
        if (!userIds.isEmpty()) {
            userRepository.findAllByUuidIn(userIds).forEach(user ->
                    userNames.put(user.getUuid(), user.getFullName()));
        }

        Map<Long, String> memberNames = new HashMap<>();
        UUID tenantId = requireTenantId();
        for (Long memberId : memberIds) {
            if (memberNames.containsKey(memberId)) {
                continue;
            }
            Optional<? extends BaseMember> member = memberRepository.findByIdAndTenantId(memberId, tenantId)
                    .map(m -> (BaseMember) m);
            if (member.isEmpty()) {
                member = childRepository.findByIdAndTenantId(memberId, tenantId).map(m -> (BaseMember) m);
            }
            member.ifPresent(baseMember -> memberNames.put(memberId, buildMemberDisplayName(baseMember)));
        }

        for (AppointmentEntity appointment : appointments) {
            if (appointment.getAssignments() != null) {
                for (AppointmentAssignmentEntity assignment : appointment.getAssignments()) {
                    String resolved = userNames.get(assignment.getUserId());
                    if (resolved != null && (assignment.getFullName() == null || assignment.getFullName().isBlank())) {
                        assignment.setFullName(resolved);
                    }
                }
            }
            if (appointment.getParticipants() != null) {
                for (AppointmentParticipantEntity participant : appointment.getParticipants()) {
                    if (participant.getMemberId() != null) {
                        String resolved = memberNames.get(participant.getMemberId());
                        if (resolved != null && (participant.getFullName() == null || participant.getFullName().isBlank())) {
                            participant.setFullName(resolved);
                        }
                    }
                }
            }
        }
    }

    private String buildMemberDisplayName(BaseMember member) {
        String first = Optional.ofNullable(member.getFirstName()).orElse("");
        String father = Optional.ofNullable(member.getFatherName()).orElse("");
        String grand = Optional.ofNullable(member.getGrandFatherName()).orElse("");
        return String.join(" ", List.of(first, father, grand).stream()
                .filter(part -> !part.isBlank())
                .toList());
    }

    private void ensureNoConflicts(UUID tenantId,
                                   Set<AppointmentAssignmentEntity> assignments,
                                   Instant start,
                                   Instant end,
                                   UUID excludeAppointmentId) {
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        Instant normalizedEnd = end != null ? end : start.plusSeconds(1);
        Set<UUID> userIds = assignments.stream()
                .map(AppointmentAssignmentEntity::getUserId)
                .collect(Collectors.toSet());
        Set<AppointmentStatus> excluded = Set.of(
                AppointmentStatus.CANCELLED,
                AppointmentStatus.COMPLETED,
                AppointmentStatus.NO_SHOW
        );
        List<AppointmentEntity> conflicts = appointmentRepository.findConflicts(
                tenantId,
                userIds,
                start,
                normalizedEnd,
                excludeAppointmentId,
                excluded
        );
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Assignee has a conflicting appointment in the selected time range");
        }
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID not found in context while managing appointments");
        }
        return tenantId;
    }

    private ChurchEntity resolveChurch(UUID tenantId) {
        return churchRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Church not found for tenant"));
    }

    private void validateRequest(AppointmentCreateRequest request) {
        if (request.endDateTime() != null && request.endDateTime().isBefore(request.startDateTime())) {
            throw new IllegalArgumentException("endDateTime must be after startDateTime");
        }
    }

    private void validateReschedule(AppointmentRescheduleRequest request) {
        if (request.newEnd() != null && request.newEnd().isBefore(request.newStart())) {
            throw new IllegalArgumentException("newEnd must be after newStart");
        }
    }
}
