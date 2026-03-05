package com.anastasia.Anastasia_BackEnd.core.notification.repository;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    @Query("""
        select n
        from NotificationEntity n
        where n.recipientUserId = :userId
          and n.channel = :channel
          and n.archived = false
          and (
            (:tenantId is null and n.tenant is null)
            or n.tenant.id = :tenantId
          )
        order by n.createdAt desc
    """)
    Page<NotificationEntity> findInbox(
            @Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId,
            @Param("channel") NotificationChannelType channel,
            Pageable pageable
    );

    @Query("""
        select n
        from NotificationEntity n
        where n.recipientUserId = :userId
          and n.channel = :channel
          and n.archived = false
          and n.type in :types
          and (
            (:tenantId is null and n.tenant is null)
            or n.tenant.id = :tenantId
          )
        order by n.createdAt desc
    """)
    Page<NotificationEntity> findInboxByTypes(
            @Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId,
            @Param("channel") NotificationChannelType channel,
            @Param("types") Set<NotificationType> types,
            Pageable pageable
    );

    @Query("""
        select count(n)
        from NotificationEntity n
        where n.recipientUserId = :userId
          and n.channel = com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType.IN_APP
          and n.archived = false
          and n.readAt is null
          and (
            (:tenantId is null and n.tenant is null)
            or n.tenant.id = :tenantId
          )
    """)
    long countUnread(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    @Query("""
        select n
        from NotificationEntity n
        where n.id = :id
          and n.recipientUserId = :userId
          and n.archived = false
          and (
            (:tenantId is null and n.tenant is null)
            or n.tenant.id = :tenantId
          )
    """)
    Optional<NotificationEntity> findByIdAndScope(@Param("id") Long id,
                                                   @Param("userId") UUID userId,
                                                   @Param("tenantId") UUID tenantId);

    @Query("""
        update NotificationEntity n
        set n.readAt = CURRENT_TIMESTAMP, n.updatedAt = CURRENT_TIMESTAMP
        where n.recipientUserId = :userId
          and n.channel = com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType.IN_APP
          and n.archived = false
          and n.readAt is null
          and (
            (:tenantId is null and n.tenant is null)
            or n.tenant.id = :tenantId
          )
    """)
    @Modifying
    int markAllRead(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    boolean existsByIdempotencyKeyAndChannel(String idempotencyKey, NotificationChannelType channel);
}
