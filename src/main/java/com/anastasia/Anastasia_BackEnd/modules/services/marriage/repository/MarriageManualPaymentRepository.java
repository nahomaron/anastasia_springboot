package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageManualPaymentEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageManualPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarriageManualPaymentRepository extends JpaRepository<MarriageManualPaymentEntity, UUID> {
    List<MarriageManualPaymentEntity> findByMarriageCaseId(UUID marriageCaseId);
    List<MarriageManualPaymentEntity> findByMarriageCaseIdAndVerificationStatus(UUID marriageCaseId, MarriageManualPaymentStatus status);
}
