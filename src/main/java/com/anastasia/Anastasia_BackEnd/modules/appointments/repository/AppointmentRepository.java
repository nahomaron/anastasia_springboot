package com.anastasia.Anastasia_BackEnd.modules.appointments.repository;

import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatus;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {

    long countByTenantId(UUID tenantId);

    @EntityGraph(attributePaths = {"participants", "assignments", "statusHistory"})
    Optional<AppointmentEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    @EntityGraph(attributePaths = {"participants", "assignments"})
    @Query("""
            select distinct a from AppointmentEntity a
            join a.participants p
            where a.id = :appointmentId
              and a.tenantId = :tenantId
              and p.memberId in :memberIds
            """)
    Optional<AppointmentEntity> findMemberVisibleByIdAndTenantId(
            @Param("appointmentId") UUID appointmentId,
            @Param("tenantId") UUID tenantId,
            @Param("memberIds") Set<Long> memberIds
    );

    @EntityGraph(attributePaths = {"participants", "assignments"})
    @Query("select a from AppointmentEntity a where a.tenantId = :tenantId and a.startAtUtc >= :start and a.startAtUtc <= :end")
    List<AppointmentEntity> findForRange(
            @Param("tenantId") UUID tenantId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("select count(a) from AppointmentEntity a join a.participants p where a.tenantId = :tenantId and p.memberId = :memberId")
    long countByTenantIdAndMemberId(@Param("tenantId") UUID tenantId, @Param("memberId") Long memberId);

    @EntityGraph(attributePaths = {"participants", "assignments"})
    List<AppointmentEntity> findByTenantIdAndStatus(UUID tenantId, AppointmentStatus status);

    @EntityGraph(attributePaths = {"participants", "assignments"})
    List<AppointmentEntity> findByTenantIdAndType(UUID tenantId, AppointmentType type);

    @EntityGraph(attributePaths = {"participants", "assignments"})
    List<AppointmentEntity> findByTenantId(UUID tenantId);

    @EntityGraph(attributePaths = {"participants", "assignments", "statusHistory"})
    List<AppointmentEntity> findByTenantIdOrderByStartAtUtcAsc(UUID tenantId);

    @EntityGraph(attributePaths = {"participants", "assignments"})
    @Query("""
            select distinct a from AppointmentEntity a
            join a.participants p
            where a.tenantId = :tenantId
              and p.memberId in :memberIds
              and (:start is null or a.startAtUtc >= :start)
              and (:end is null or a.startAtUtc <= :end)
              and (:status is null or a.status = :status)
              and (:type is null or a.type = :type)
            order by a.startAtUtc asc
            """)
    List<AppointmentEntity> findMemberVisibleAppointments(
            @Param("tenantId") UUID tenantId,
            @Param("memberIds") Set<Long> memberIds,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("status") AppointmentStatus status,
            @Param("type") AppointmentType type
    );

    @Query("""
            select distinct a from AppointmentEntity a
            join a.assignments assign
            where a.tenantId = :tenantId
              and assign.userId in :userIds
              and (:excludeId is null or a.id <> :excludeId)
              and a.status not in :excludedStatuses
              and a.startAtUtc < :end
              and coalesce(a.endAtUtc, a.startAtUtc) > :start
            """)
    List<AppointmentEntity> findConflicts(
            @Param("tenantId") UUID tenantId,
            @Param("userIds") Set<UUID> userIds,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("excludeId") UUID excludeId,
            @Param("excludedStatuses") Set<AppointmentStatus> excludedStatuses
    );
}
