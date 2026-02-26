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

    Page<NotificationEntity> findByRecipientUserIdAndTenant_IdAndChannelAndArchivedFalseOrderByCreatedAtDesc(
            UUID recipientUserId,
            UUID tenantId,
            NotificationChannelType channel,
            Pageable pageable
    );

    Page<NotificationEntity> findByRecipientUserIdAndTenant_IdAndChannelAndTypeInAndArchivedFalseOrderByCreatedAtDesc(
            UUID recipientUserId,
            UUID tenantId,
            NotificationChannelType channel,
            Set<NotificationType> types,
            Pageable pageable
    );

    @Query("""
        select count(n)
        from NotificationEntity n
        where n.recipientUserId = :userId
          and n.tenant.id = :tenantId
          and n.channel = com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType.IN_APP
          and n.archived = false
          and n.readAt is null
    """)
    long countUnread(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    Optional<NotificationEntity> findByIdAndRecipientUserIdAndTenant_IdAndArchivedFalse(Long id, UUID userId, UUID tenantId);

    @Query("""
        update NotificationEntity n
        set n.readAt = CURRENT_TIMESTAMP, n.updatedAt = CURRENT_TIMESTAMP
        where n.recipientUserId = :userId
          and n.tenant.id = :tenantId
          and n.channel = com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType.IN_APP
          and n.archived = false
          and n.readAt is null
    """)
    @Modifying
    int markAllRead(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    boolean existsByIdempotencyKeyAndChannel(String idempotencyKey, NotificationChannelType channel);
}
