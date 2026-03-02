package com.anastasia.Anastasia_BackEnd.modules.groups;

import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, Long> {

    boolean existsByGroupName(String groupName);

    boolean existsByGroupNameAndTenantId(String groupName, UUID tenantId);

    Page<GroupEntity> findAllByTenantId(UUID tenantId, Pageable pageable);

    Page<GroupEntity> findAllByCreatedByAndTenantId(UUID createdBy, UUID tenantId, Pageable pageable);

    @Query("""
        select distinct g
        from GroupEntity g
        left join g.users u with u.uuid = :userId
        left join g.managers m with m.uuid = :userId
        where g.tenantId = :tenantId and (
            upper(coalesce(g.visibility, '')) in ('PUBLIC', 'ALL')
            or g.createdBy = :userId
            or u.uuid is not null
            or m.uuid is not null
        )
    """)
    Page<GroupEntity> findVisibleForUser(@Param("tenantId") UUID tenantId,
                                         @Param("userId") UUID userId,
                                         Pageable pageable);

    @Query("""
        select distinct g
        from GroupEntity g
        left join g.users u with u.uuid = :userId
        left join g.managers m with m.uuid = :userId
        where g.tenantId = :tenantId
          and g.groupId = :groupId
          and (
            upper(coalesce(g.visibility, '')) in ('PUBLIC', 'ALL')
            or g.createdBy = :userId
            or u.uuid is not null
            or m.uuid is not null
          )
    """)
    Optional<GroupEntity> findVisibleByIdForUser(@Param("tenantId") UUID tenantId,
                                                 @Param("groupId") Long groupId,
                                                 @Param("userId") UUID userId);

}
