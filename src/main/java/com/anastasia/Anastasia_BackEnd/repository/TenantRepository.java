package com.anastasia.Anastasia_BackEnd.repository;

import com.anastasia.Anastasia_BackEnd.model.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {

}
