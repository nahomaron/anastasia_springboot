package com.anastasia.Anastasia_BackEnd.modules.users.repository;

import com.anastasia.Anastasia_BackEnd.modules.users.model.UserRecoveryEmailVerificationCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRecoveryEmailVerificationCodeRepository extends JpaRepository<UserRecoveryEmailVerificationCodeEntity, Long> {
    Optional<UserRecoveryEmailVerificationCodeEntity> findByEmailIgnoreCase(String email);
}
