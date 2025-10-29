package com.anastasia.Anastasia_BackEnd.modules.accounting.repository;

import com.anastasia.Anastasia_BackEnd.modules.accounting.model.LedgerEntry;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByAccountId(Long accountId);

    @Query("""
            select le.account.id as accountId,
                   sum(le.debit) as totalDebit,
                   sum(le.credit) as totalCredit
            from LedgerEntry le
            where le.account.tenantId = :tenantId
              and le.account.type in :accountTypes
              and (:startDate is null or le.transaction.date >= :startDate)
              and (:endDate is null or le.transaction.date <= :endDate)
            group by le.account.id
            """)
    List<AccountBalanceView> aggregateByAccountAndPeriod(@Param("tenantId") String tenantId,
                                                         @Param("accountTypes") List<AccountType> accountTypes,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    @Query("""
            select le from LedgerEntry le
            join fetch le.transaction t
            where le.account.id = :accountId
              and (:startDate is null or t.date >= :startDate)
              and (:endDate is null or t.date <= :endDate)
            order by t.date asc
            """)
    List<LedgerEntry> findByAccountAndDateRange(@Param("accountId") Long accountId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    interface AccountBalanceView {
        Long getAccountId();

        java.math.BigDecimal getTotalDebit();

        java.math.BigDecimal getTotalCredit();
    }
}
