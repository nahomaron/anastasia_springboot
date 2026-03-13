package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCertificateAmendmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarriageCertificateAmendmentRepository extends JpaRepository<MarriageCertificateAmendmentEntity, UUID> {
    List<MarriageCertificateAmendmentEntity> findByCertificateIdOrderByAmendedAtDesc(UUID certificateId);
}
