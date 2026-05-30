package com.anastasia.Anastasia_BackEnd.core.auth.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformAdminBootstrapAuditRepository extends JpaRepository<PlatformAdminBootstrapAuditEvent, Long> {
}
