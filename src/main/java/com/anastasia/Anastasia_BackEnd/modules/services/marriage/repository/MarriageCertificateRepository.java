package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCertificateEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCertificateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarriageCertificateRepository extends JpaRepository<MarriageCertificateEntity, UUID> {
    boolean existsByCertificateNumber(String certificateNumber);
    Optional<MarriageCertificateEntity> findFirstByMarriageCaseIdOrderByIssuedDateDesc(UUID marriageCaseId);
    List<MarriageCertificateEntity> findByStatusOrderByIssuedDateDesc(MarriageCertificateStatus status);
}
