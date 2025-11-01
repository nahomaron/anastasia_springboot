package com.anastasia.Anastasia_BackEnd.core.notification.repository;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
}
