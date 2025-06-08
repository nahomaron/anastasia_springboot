package com.anastasia.Anastasia_BackEnd.repository;

import com.anastasia.Anastasia_BackEnd.model.priest.PriestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PriestRepository extends JpaRepository<PriestEntity, Long> {
    boolean existsByPriestNumber(String priestNumber);

    Optional<PriestEntity> findByPhoneNumber(String phoneNumber);
}
