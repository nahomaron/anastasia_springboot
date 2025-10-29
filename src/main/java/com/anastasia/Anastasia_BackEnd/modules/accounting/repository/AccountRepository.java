package com.anastasia.Anastasia_BackEnd.modules.accounting.repository;

import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByTenantId(String tenantId);

    List<Account> findByTenantIdAndType(String tenantId, AccountType type);

    Optional<Account> findByIdAndTenantId(Long id, String tenantId);

    Optional<Account> findByTenantIdAndCode(String tenantId, String code);

    Optional<Account> findByTenantIdAndNameIgnoreCase(String tenantId, String name);

    /**
     * This is a critical method for concurrent transactions.
     * It acquires a pessimistic write lock on the account row to prevent
     * race conditions when updating balances.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.tenantId = :tenantId")
    Optional<Account> findByIdAndTenantIdForUpdate(@Param("id") Long id, @Param("tenantId") String tenantId);
}
