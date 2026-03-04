package com.anastasia.Anastasia_BackEnd.core.auth.repository;

import com.anastasia.Anastasia_BackEnd.core.auth.model.LoginTwoFactorChallengeEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface LoginTwoFactorChallengeRepository extends JpaRepository<LoginTwoFactorChallengeEntity, String> {

    Optional<LoginTwoFactorChallengeEntity> findByChallengeToken(String challengeToken);

    @Transactional
    @Modifying
    @Query("delete from LoginTwoFactorChallengeEntity c where c.user.uuid = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Transactional
    @Modifying
    @Query("delete from LoginTwoFactorChallengeEntity c where c.expiresAt < :now or c.consumedAt is not null")
    void deleteExpiredOrConsumed(@Param("now") LocalDateTime now);
}
