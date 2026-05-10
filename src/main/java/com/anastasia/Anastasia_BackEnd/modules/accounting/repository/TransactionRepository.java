package com.anastasia.Anastasia_BackEnd.modules.accounting.repository;

import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByTenantId(UUID tenantId);

    List<Transaction> findByTenantIdAndDateBetween(UUID tenantId, LocalDate startDate, LocalDate endDate);

    Optional<Transaction> findByIdAndTenantId(Long id, UUID tenantId);

    @Query("""
            select distinct t
            from Transaction t
            left join fetch t.ledgerEntries le
            left join fetch le.account
            left join fetch le.fund
            where t.tenantId = :tenantId
              and (:startDate is null or t.date >= :startDate)
              and (:endDate is null or t.date <= :endDate)
              and (:accountId is null or exists (
                    select 1
                    from LedgerEntry filterEntry
                    where filterEntry.transaction = t
                      and filterEntry.account.id = :accountId
              ))
            order by t.date desc, t.id desc
            """)
    List<Transaction> findVisibleTransactions(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("accountId") Long accountId
    );

    Optional<Transaction> findByTenantIdAndExternalReferenceAndSourceSystem(UUID tenantId, String externalReference, String sourceSystem);
}
