package com.anastasia.Anastasia_BackEnd.modules.users.repository;

import com.anastasia.Anastasia_BackEnd.modules.users.model.UserPreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserPreferencesRepository extends JpaRepository<UserPreferencesEntity, UUID> {
}
