package com.anastasia.Anastasia_BackEnd.modules.appointments.mappers;

import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentAssigneeResponse;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentParticipantResponse;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentResponse;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentStatusHistoryResponse;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentParticipantEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatusHistoryEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AssignedRole;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.ParticipantRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentMapper {

    AppointmentParticipantResponse toParticipantResponse(AppointmentParticipantEntity entity);

    AppointmentAssigneeResponse toAssigneeResponse(AppointmentAssignmentEntity entity);

    AppointmentStatusHistoryResponse toHistoryResponse(AppointmentStatusHistoryEntity entity);

    @Mapping(target = "startDateTime", source = "startAtUtc")
    @Mapping(target = "endDateTime", source = "endAtUtc")
    @Mapping(target = "timeZone", source = "timezone")
    @Mapping(target = "privateNotesExists", source = "privateNotesExists")
    @Mapping(target = "firstVisit", source = "firstVisit")
    @Mapping(target = "sacramentRelated", source = "sacramentRelated")
    @Mapping(target = "participants", expression = "java(mapParticipants(entity))")
    @Mapping(target = "assignees", expression = "java(mapAssignees(entity))")
    @Mapping(target = "member", expression = "java(resolvePrimaryMember(entity))")
    @Mapping(target = "primaryAssignee", expression = "java(resolvePrimaryAssignee(entity))")
    @Mapping(target = "statusHistory", expression = "java(mapStatusHistory(entity))")
    AppointmentResponse toResponse(AppointmentEntity entity);

    default Set<AppointmentParticipantResponse> mapParticipants(AppointmentEntity entity) {
        if (entity.getParticipants() == null) {
            return Set.of();
        }
        return entity.getParticipants().stream()
                .sorted(Comparator.comparing(AppointmentParticipantEntity::getRole))
                .map(this::toParticipantResponse)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    default Set<AppointmentAssigneeResponse> mapAssignees(AppointmentEntity entity) {
        if (entity.getAssignments() == null) {
            return Set.of();
        }
        return entity.getAssignments().stream()
                .sorted(Comparator.comparing(AppointmentAssignmentEntity::getRole))
                .map(this::toAssigneeResponse)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    default AppointmentParticipantResponse resolvePrimaryMember(AppointmentEntity entity) {
        if (entity.getParticipants() == null) {
            return null;
        }
        return entity.getParticipants().stream()
                .filter(p -> p.getRole() == ParticipantRole.MEMBER)
                .findFirst()
                .map(this::toParticipantResponse)
                .orElse(null);
    }

    default AppointmentAssigneeResponse resolvePrimaryAssignee(AppointmentEntity entity) {
        if (entity.getAssignments() == null) {
            return null;
        }
        return entity.getAssignments().stream()
                .sorted(Comparator.comparing((AppointmentAssignmentEntity a) -> a.getRole() == AssignedRole.PRIEST ? 0 : 1))
                .findFirst()
                .map(this::toAssigneeResponse)
                .orElse(null);
    }

    default Set<AppointmentStatusHistoryResponse> mapStatusHistory(AppointmentEntity entity) {
        if (entity.getStatusHistory() == null) {
            return Set.of();
        }
        return entity.getStatusHistory().stream()
                .sorted(Comparator.comparing(AppointmentStatusHistoryEntity::getChangedAt))
                .map(this::toHistoryResponse)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
