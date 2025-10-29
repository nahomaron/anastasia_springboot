package com.anastasia.Anastasia_BackEnd.modules.accounting.repository;

import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByTenantId(String tenantId);

    List<Transaction> findByTenantIdAndDateBetween(String tenantId, LocalDate startDate, LocalDate endDate);

    Optional<Transaction> findByTenantIdAndExternalReferenceAndSourceSystem(String tenantId, String externalReference, String sourceSystem);
}
