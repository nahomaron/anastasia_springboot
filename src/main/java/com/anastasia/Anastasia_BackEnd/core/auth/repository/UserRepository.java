package com.anastasia.Anastasia_BackEnd.core.auth.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {

    // Basic lookups
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByEmailIgnoreCase(String email);
    @Query("""
        SELECT u
        FROM UserEntity u
        WHERE u.affiliatedTenantId = :tenantId
          AND LOWER(u.email) = LOWER(:email)
    """)
    Optional<UserEntity> findByTenantIdAndEmailIgnoreCase(@Param("tenantId") UUID tenantId, @Param("email") String email);
    Optional<UserEntity> findByGoogleId(String googleId);
    boolean existsByEmail(String email);

    @Query("""
        SELECT DISTINCT u
        FROM UserEntity u
        JOIN u.roles r
        WHERE r.roleName IN :roleNames
        ORDER BY u.createdAt DESC, u.fullName ASC
    """)
    List<UserEntity> findAllByRoleNames(@Param("roleNames") Set<String> roleNames);

    // Find users by group
//    @Query("SELECT u FROM UserEntity u JOIN u.groups g WHERE g.groupId = :groupId")
//    Page<UserEntity> findUsersByGroupId(@Param("groupId") Long groupId, Pageable pageable);

    @Query("""
        SELECT new com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO(u.uuid, u.fullName, u.email)
        FROM UserEntity u
        JOIN u.groups g
        WHERE g.groupId = :groupId
    """)
    Page<SimpleUserDTO> findUsersByGroupId(@Param("groupId") Long groupId, Pageable pageable);

    // Find users by a list of UUIDs
    List<UserEntity> findAllByUuidIn(Set<UUID> uuids);

    @Query("""
        SELECT new com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO(u.uuid, u.fullName, u.email)
        FROM UserEntity u
        WHERE u.affiliatedTenantId = :tenantId
          AND (
            LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        ORDER BY u.fullName
    """)
    List<SimpleUserDTO> searchByTenantId(@Param("tenantId") UUID tenantId, @Param("q") String query);

    @Query("""
        SELECT DISTINCT new com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO(u.uuid, u.fullName, u.email)
        FROM UserEntity u
        JOIN u.roles r
        WHERE u.affiliatedTenantId = :tenantId
          AND r.roleName IN :roles
          AND (
            LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        ORDER BY u.fullName
    """)
    List<SimpleUserDTO> searchByTenantIdAndRoles(
            @Param("tenantId") UUID tenantId,
            @Param("q") String query,
            @Param("roles") Set<String> roles
    );

    // --- Church-based queries (optimized) ---

    @Query("""
        SELECT u FROM UserEntity u
        WHERE u.membership.church.churchId = :churchId
    """)
    List<UserEntity> findAllByChurchId(@Param("churchId") Long churchId);

    @Query("""
        SELECT u FROM UserEntity u
        JOIN Adult_MemberEntity m ON u.membershipId = m.id
        WHERE m.church.churchId = :churchId
        AND u.membershipId IS NOT NULL
    """)
    List<UserEntity> findAllUsersByChurchIdOptimized(@Param("churchId") Long churchId);

    @Query("""
        SELECT u.uuid FROM UserEntity u
        JOIN Adult_MemberEntity m ON u.membershipId = m.id
        WHERE m.church.churchId = :churchId
        AND u.membershipId IS NOT NULL
    """)
    List<UUID> findUserUUIDsByChurchId(@Param("churchId") Long churchId);

    @Query("""
        SELECT new com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO(u.uuid, u.fullName, u.email)
        FROM UserEntity u
        JOIN Adult_MemberEntity m ON u.membershipId = m.id
        WHERE m.church.churchId = :churchId
        AND u.membershipId IS NOT NULL
    """)
    List<SimpleUserDTO> findSimpleUsersByChurchId(@Param("churchId") Long churchId);

    @Query("""
        SELECT new com.anastasia.Anastasia_BackEnd.modules.users.model.SimpleUserDTO(u.uuid, u.fullName, u.email)
        FROM UserEntity u
        JOIN Adult_MemberEntity m ON u.membershipId = m.id
        WHERE m.church.churchId = :churchId
          AND u.membershipId IS NOT NULL
          AND (
            LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        ORDER BY u.fullName
    """)
    List<SimpleUserDTO> searchSimpleUsersByChurchId(@Param("churchId") Long churchId, @Param("q") String query);


    List<UserEntity> findAllByEmailIn(Set<String> groupEmail);

    List<UserEntity> findByAffiliatedTenantId(UUID tenantId);

    Page<UserEntity> findByAffiliatedTenantId(UUID tenantId, Pageable pageable);

    Optional<UserEntity> findByUuidAndAffiliatedTenantId(UUID userId, UUID tenantId);

    default List<UserEntity> findByTenantId(UUID tenantId) {
        return findByAffiliatedTenantId(tenantId);
    }

    long countByRoles_Id(Long roleId);

    long countByRoles_IdAndAffiliatedTenantId(Long roleId, UUID tenantId);

    @Query("SELECT u FROM UserEntity u WHERE u.affiliatedTenant.id = :tenantId AND u.userType = 'TENANT'")
    Optional<UserEntity> findTenantAdmin(UUID tenantId);

}
