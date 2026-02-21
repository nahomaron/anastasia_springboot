package com.anastasia.Anastasia_BackEnd.modules.groups;

import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, Long> {

    boolean existsByGroupName(String groupName);

    boolean existsByGroupNameAndTenantId(String groupName, UUID tenantId);

    Page<GroupEntity> findAllByTenantId(UUID tenantId, Pageable pageable);

    Page<GroupEntity> findAllByCreatedByAndTenantId(UUID createdBy, UUID tenantId, Pageable pageable);

}
