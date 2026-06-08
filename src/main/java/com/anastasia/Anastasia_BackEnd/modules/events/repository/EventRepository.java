package com.anastasia.Anastasia_BackEnd.modules.events.repository;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventManagerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {
    long countByTenantId(UUID tenantId);

    List<EventEntity> findByTenantId(UUID tenantId);

    Optional<EventEntity> findByEventIdAndTenantId(Long eventId, UUID tenantId);

    Optional<EventEntity> findFirstByTenantIdAndChurch_ChurchIdAndTitleIgnoreCase(UUID tenantId,
                                                                                  Long churchId,
                                                                                  String title);

    List<EventManagerEntity> findAllManagersByEventId(Long eventId);

    @Query("""
        select distinct e
        from EventEntity e
        left join e.invitedGroups g
        left join g.users gu with gu.uuid = :userId
        left join g.managers gm with gm.uuid = :userId
        left join e.invitedUsers iu with iu.uuid = :userId
        left join e.invitedEmails ie with lower(ie) = lower(:userEmail)
        left join e.eventManagers em
        left join em.user mu with mu.uuid = :userId
        where e.tenantId = :tenantId and (
            e.createdBy = :userId
            or
            e.visibility = com.anastasia.Anastasia_BackEnd.modules.events.model.EventVisibilityType.ALL
            or (e.visibility = com.anastasia.Anastasia_BackEnd.modules.events.model.EventVisibilityType.GROUPS and gu.uuid is not null)
            or (e.visibility = com.anastasia.Anastasia_BackEnd.modules.events.model.EventVisibilityType.GROUPS and gm.uuid is not null)
            or (e.visibility = com.anastasia.Anastasia_BackEnd.modules.events.model.EventVisibilityType.INVITEES and (iu.uuid is not null or ie is not null))
            or (e.visibility = com.anastasia.Anastasia_BackEnd.modules.events.model.EventVisibilityType.MANAGERS and mu.uuid is not null)
        )
    """)
    List<EventEntity> findVisibleForUser(@Param("tenantId") UUID tenantId,
                                         @Param("userId") UUID userId,
                                         @Param("userEmail") String userEmail);
}
