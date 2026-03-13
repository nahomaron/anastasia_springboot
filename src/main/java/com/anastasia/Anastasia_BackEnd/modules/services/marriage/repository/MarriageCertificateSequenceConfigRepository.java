package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCertificateSequenceConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MarriageCertificateSequenceConfigRepository extends JpaRepository<MarriageCertificateSequenceConfigEntity, UUID> {
    Optional<MarriageCertificateSequenceConfigEntity> findFirstByChurch_ChurchIdAndActiveTrue(Long churchId);
}
