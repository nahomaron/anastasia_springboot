package com.anastasia.Anastasia_BackEnd.modules.users.repository;

import com.anastasia.Anastasia_BackEnd.modules.users.model.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
    List<UserProfileEntity> findByUserIdIn(Collection<UUID> userIds);
}
