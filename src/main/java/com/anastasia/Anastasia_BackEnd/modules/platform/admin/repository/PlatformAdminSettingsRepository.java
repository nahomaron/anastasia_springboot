package com.anastasia.Anastasia_BackEnd.modules.platform.admin.repository;

import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.PlatformAdminSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PlatformAdminSettingsRepository extends JpaRepository<PlatformAdminSettingsEntity, UUID> {
}
