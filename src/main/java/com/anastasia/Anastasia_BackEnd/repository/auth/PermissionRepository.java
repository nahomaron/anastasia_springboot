package com.anastasia.Anastasia_BackEnd.repository.auth;

import com.anastasia.Anastasia_BackEnd.model.permission.Permission;
import com.anastasia.Anastasia_BackEnd.model.permission.PermissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(PermissionType name);

    boolean existsByName(PermissionType perm);

    Set<Permission> findByNameIn(Set<String> permissionNames);
}
