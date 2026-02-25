package com.anastasia.Anastasia_BackEnd.core.auth.repository;

import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleName(String roleName);

    List<Role> findByTenantId(UUID tenantId);

    @Query("""
        SELECT r
        FROM Role r
        WHERE r.tenantId IS NULL OR r.tenantId = :tenantId
        ORDER BY r.roleName
    """)
    List<Role> findSystemAndTenantRoles(@Param("tenantId") UUID tenantId);

    Optional<Role> findByIdAndTenantId(Long id, UUID tenantId);

    boolean existsByRoleName(String user);

    boolean existsByRoleNameAndTenantId(String roleName, UUID tenantId);
}
