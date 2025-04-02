package com.anastasia.Anastasia_BackEnd.repository;

import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChurchRepository extends JpaRepository<ChurchEntity, Long> {

    Optional<ChurchEntity> findByChurchNumber(String churchNumber);

    boolean existsByChurchNumber(String churchNumber);
}
