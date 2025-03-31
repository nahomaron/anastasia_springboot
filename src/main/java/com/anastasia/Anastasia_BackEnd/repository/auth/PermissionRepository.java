package com.anastasia.Anastasia_BackEnd.repository.auth;

import com.anastasia.Anastasia_BackEnd.model.entity.auth.Permission;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.PermissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(String name);

    boolean existsByName(PermissionType perm);
}
