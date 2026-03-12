package com.anastasia.Anastasia_BackEnd.modules.groups;

import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupJoinRequestEntity;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupJoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequestEntity, Long> {

    Optional<GroupJoinRequestEntity> findFirstByGroup_GroupIdAndRequester_UuidOrderByCreatedDateDesc(Long groupId, UUID requesterId);

    Optional<GroupJoinRequestEntity> findFirstByGroup_GroupIdAndRequester_UuidAndStatusInOrderByCreatedDateDesc(
            Long groupId,
            UUID requesterId,
            Collection<GroupJoinRequestStatus> statuses
    );

    List<GroupJoinRequestEntity> findByGroup_GroupIdAndStatusOrderByCreatedDateAsc(Long groupId, GroupJoinRequestStatus status);

    List<GroupJoinRequestEntity> findByRequester_UuidAndTenantIdAndStatusOrderByCreatedDateDesc(
            UUID requesterId,
            UUID tenantId,
            GroupJoinRequestStatus status
    );
}
