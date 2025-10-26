package com.anastasia.Anastasia_BackEnd.modules.notification.repository;

import com.anastasia.Anastasia_BackEnd.modules.notification.domain.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
}
