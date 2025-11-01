package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AvatarRepository extends JpaRepository<AvatarEntity, Long> {

    Optional<AvatarEntity> findByOwnerId(UUID userId);

    Optional<AvatarEntity> findByOwnerIdAndAvatarType(UUID ownerId, AvatarType avatarType);
//    Optional<AvatarEntity> findByMemberId(UUID memberId);

}
