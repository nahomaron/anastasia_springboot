package com.anastasia.Anastasia_BackEnd.repository.auth;

import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String username);

    Optional<UserEntity> findByGoogleId(String googleId);

    Optional<UserEntity> findByEmailAndTenantId(String username, UUID tenantId);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserEntity u JOIN u.groups g WHERE g.groupId = :groupId")
    Page<UserEntity> findUsersByGroupId(Long groupId, Pageable pageable);

    List<UserEntity> findByUuidIn(Set<UUID> managerIds);
}
