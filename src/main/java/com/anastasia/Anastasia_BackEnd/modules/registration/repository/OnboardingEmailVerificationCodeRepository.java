package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.OnboardingEmailVerificationCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingEmailVerificationCodeRepository extends JpaRepository<OnboardingEmailVerificationCodeEntity, Long> {
    Optional<OnboardingEmailVerificationCodeEntity> findByEmailIgnoreCase(String email);
}
