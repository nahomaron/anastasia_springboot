package com.anastasia.Anastasia_BackEnd.core.auth.repository;

import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(PermissionType name);

    boolean existsByName(PermissionType perm);

    Set<Permission> findByNameIn(Set<PermissionType> permissionNames);

    @Query("""
        SELECT p
        FROM Permission p
        WHERE p.name IN :permissionTypes
    """)
    Set<Permission> findAllByPermissionTypes(@Param("permissionTypes") Set<PermissionType> permissionTypes);
}
