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
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByTenantId(UUID tenantId);

    List<Account> findByTenantIdAndType(UUID tenantId, AccountType type);

    Optional<Account> findByIdAndTenantId(Long id, UUID tenantId);

    Optional<Account> findByTenantIdAndCode(UUID tenantId, String code);

    Optional<Account> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);

    /**
     * This is a critical method for concurrent transactions.
     * It acquires a pessimistic write lock on the account row to prevent
     * race conditions when updating balances.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.tenantId = :tenantId")
    Optional<Account> findByIdAndTenantIdForUpdate(@Param("id") Long id, @Param("tenantId") UUID tenantId);
}
