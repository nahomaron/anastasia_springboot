package com.anastasia.Anastasia_BackEnd.modules.users.repository;

import com.anastasia.Anastasia_BackEnd.modules.users.model.UserTwoFactorBackupCodeEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserTwoFactorBackupCodeRepository extends JpaRepository<UserTwoFactorBackupCodeEntity, Long> {

    @Transactional
    @Modifying
    @Query("delete from UserTwoFactorBackupCodeEntity c where c.user.uuid = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Query("""
            select c from UserTwoFactorBackupCodeEntity c
            where c.user.uuid = :userId and c.usedAt is null
            """)
    List<UserTwoFactorBackupCodeEntity> findActiveByUserId(@Param("userId") UUID userId);

    @Query("""
            select count(c) from UserTwoFactorBackupCodeEntity c
            where c.user.uuid = :userId and c.usedAt is null
            """)
    long countUnusedByUserId(@Param("userId") UUID userId);
}
