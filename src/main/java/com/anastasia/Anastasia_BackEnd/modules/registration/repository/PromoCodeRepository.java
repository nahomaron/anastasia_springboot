package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.PromoCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PromoCodeRepository extends JpaRepository<PromoCodeEntity, UUID> {
    Optional<PromoCodeEntity> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
