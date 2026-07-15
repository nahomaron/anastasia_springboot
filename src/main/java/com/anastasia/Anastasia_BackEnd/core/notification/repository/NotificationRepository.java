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

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    @Query("""
        select n
        from NotificationEntity n
        where n.recipientUserId = :userId
          and n.channel = :channel
          and n.archivedAt is null
          and (
            n.tenant is null
            or (:tenantId is not null and n.tenant.id = :tenantId)
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
          and n.archivedAt is null
          and n.type in :types
          and (
            n.tenant is null
            or (:tenantId is not null and n.tenant.id = :tenantId)
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
          and n.archivedAt is null
          and n.readAt is null
          and (
            n.tenant is null
            or (:tenantId is not null and n.tenant.id = :tenantId)
          )
    """)
    long countUnread(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    @Query("""
        select n
        from NotificationEntity n
        where n.id = :id
          and n.recipientUserId = :userId
          and n.archivedAt is null
          and (
            n.tenant is null
            or (:tenantId is not null and n.tenant.id = :tenantId)
          )
    """)
    Optional<NotificationEntity> findByIdAndScope(@Param("id") Long id,
                                                   @Param("userId") UUID userId,
                                                   @Param("tenantId") UUID tenantId);

    @Query("""
        update NotificationEntity n
        set n.readAt = :now, n.updatedAt = :now
        where n.recipientUserId = :userId
          and n.channel = com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType.IN_APP
          and n.archivedAt is null
          and n.readAt is null
          and (
            n.tenant is null
            or (:tenantId is not null and n.tenant.id = :tenantId)
          )
    """)
    @Modifying
    int markAllRead(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId, @Param("now") Instant now);

    boolean existsByIdempotencyKeyAndChannel(String idempotencyKey, NotificationChannelType channel);

    @Query("""
        select count(n) > 0
        from NotificationEntity n
        where n.recipientUserId = :userId
          and n.tenant.id = :tenantId
          and n.channel = com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType.IN_APP
          and n.archivedAt is null
    """)
    boolean existsActiveTenantInboxNotificationForRecipient(@Param("tenantId") UUID tenantId,
                                                           @Param("userId") UUID userId);

    @Query("""
        select count(n)
        from NotificationEntity n
        where n.tenant.id = :tenantId
          and n.channel = com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType.EMAIL
          and n.deliveryStatus = com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationDeliveryStatus.SENT
          and n.deliveredAt >= :periodStart
          and n.deliveredAt < :periodEnd
    """)
    long countSentEmailByTenantAndDeliveredAtBetween(@Param("tenantId") UUID tenantId,
                                                     @Param("periodStart") Instant periodStart,
                                                     @Param("periodEnd") Instant periodEnd);
}
