package com.anastasia.Anastasia_BackEnd.core.notification.repository;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.EmailSuppressionEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.EmailSuppressionReason;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailSuppressionRepository extends JpaRepository<EmailSuppressionEntity, Long> {

    boolean existsByEmailAndReason(String email, EmailSuppressionReason reason);

    boolean existsByEmail(String email);
}
