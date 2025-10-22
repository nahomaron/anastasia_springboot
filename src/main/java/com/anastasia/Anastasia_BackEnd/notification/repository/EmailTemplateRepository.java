package com.anastasia.Anastasia_BackEnd.notification.repository;

import com.anastasia.Anastasia_BackEnd.notification.template.EmailTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplateEntity, Long> {
    Optional<EmailTemplateEntity> findByTenant_IdAndName(UUID tenantId, String name);
}
