package com.anastasia.Anastasia_BackEnd.repository.auth;

import com.anastasia.Anastasia_BackEnd.model.user.SimpleUserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    // Basic lookups
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByGoogleId(String googleId);
    boolean existsByEmail(String email);

    // Find users by group
//    @Query("SELECT u FROM UserEntity u JOIN u.groups g WHERE g.groupId = :groupId")
//    Page<UserEntity> findUsersByGroupId(@Param("groupId") Long groupId, Pageable pageable);

    @Query("""
        SELECT new com.anastasia.Anastasia_BackEnd.model.user.SimpleUserDTO(u.uuid, u.fullName, u.email)
        FROM UserEntity u
        JOIN u.groups g
        WHERE g.groupId = :groupId
    """)
    Page<SimpleUserDTO> findUsersByGroupId(@Param("groupId") Long groupId, Pageable pageable);

    // Find users by a list of UUIDs
    List<UserEntity> findAllByUuidIn(Set<UUID> uuids);

    // --- Church-based queries (optimized) ---

    @Query("""
        SELECT u FROM UserEntity u
        WHERE u.membership.church.churchId = :churchId
    """)
    List<UserEntity> findAllByChurchId(@Param("churchId") Long churchId);

    @Query("""
        SELECT u FROM UserEntity u
        JOIN MemberEntity m ON u.membershipId = m.id
        WHERE m.church.churchId = :churchId
        AND u.membershipId IS NOT NULL
    """)
    List<UserEntity> findAllUsersByChurchIdOptimized(@Param("churchId") Long churchId);

    @Query("""
        SELECT u.uuid FROM UserEntity u
        JOIN MemberEntity m ON u.membershipId = m.id
        WHERE m.church.churchId = :churchId
        AND u.membershipId IS NOT NULL
    """)
    List<UUID> findUserUUIDsByChurchId(@Param("churchId") Long churchId);

    @Query("""
        SELECT new com.anastasia.Anastasia_BackEnd.model.user.SimpleUserDTO(u.uuid, u.fullName, u.email)
        FROM UserEntity u
        JOIN MemberEntity m ON u.membershipId = m.id
        WHERE m.church.churchId = :churchId
        AND u.membershipId IS NOT NULL
    """)
    List<SimpleUserDTO> findSimpleUsersByChurchId(@Param("churchId") Long churchId);


    List<UserEntity> findAllByEmailIn(Set<String> groupEmail);

    @Query("SELECT u FROM UserEntity u WHERE u.tenant.id = :tenantId AND u.userType = 'TENANT'")
    Optional<UserEntity> findTenantAdmin(UUID tenantId);

}
