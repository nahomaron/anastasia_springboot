package com.anastasia.Anastasia_BackEnd.repository;

import com.anastasia.Anastasia_BackEnd.model.avatar.AvatarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AvatarRepository extends JpaRepository<AvatarEntity, Long> {

    Optional<AvatarEntity> findByOwnerId(UUID userId);
//    Optional<AvatarEntity> findByMemberId(UUID memberId);

}
