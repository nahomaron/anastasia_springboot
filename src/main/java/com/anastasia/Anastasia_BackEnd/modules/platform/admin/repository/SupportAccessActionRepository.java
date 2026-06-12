package com.anastasia.Anastasia_BackEnd.modules.platform.admin.repository;

import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface SupportAccessActionRepository extends JpaRepository<SupportAccessActionEntity, UUID> {

    List<SupportAccessActionEntity> findBySession_IdInOrderByOccurredAtDesc(Set<UUID> sessionIds);
}
