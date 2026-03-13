package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePairingTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarriagePairingTokenRepository extends JpaRepository<MarriagePairingTokenEntity, UUID> {
    Optional<MarriagePairingTokenEntity> findByTokenValueAndActiveTrue(String tokenValue);
    List<MarriagePairingTokenEntity> findByMarriageCaseIdAndActiveTrue(UUID marriageCaseId);
    List<MarriagePairingTokenEntity> findByExpiresAtBeforeAndActiveTrue(Instant expiresAt);
}
