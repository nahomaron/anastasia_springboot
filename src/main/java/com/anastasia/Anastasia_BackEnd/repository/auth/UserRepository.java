package com.anastasia.Anastasia_BackEnd.repository.auth;

import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String username);

    Optional<UserEntity> findByGoogleId(String googleId);

    Optional<UserEntity> findByEmailAndTenantId(String username, UUID tenantId);

}
