package com.anastasia.Anastasia_BackEnd.repository;

import com.anastasia.Anastasia_BackEnd.model.priest.PriestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriestRepository extends JpaRepository<PriestEntity, Long> {
    boolean existsByPriestNumber(String priestNumber);
}
