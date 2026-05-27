package com.anastasia.Anastasia_BackEnd.modules.platform.admin.repository;

import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.PlatformAdminRecoveryAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformAdminRecoveryAuditRepository extends JpaRepository<PlatformAdminRecoveryAuditEvent, Long> {
}
