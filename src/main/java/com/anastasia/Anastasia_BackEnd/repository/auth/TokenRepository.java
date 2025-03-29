package com.anastasia.Anastasia_BackEnd.repository.auth;

import com.anastasia.Anastasia_BackEnd.model.entity.auth.Token;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer> {

    Optional<Token> findByToken(String token);

    @Query("""
            select t from Token t inner join UserEntity u on t.user.id = u.uuid
            where u.uuid = :uuid and (t.expired = false or t.revoked = false)
            """)
    List<Token> findAllValidUserTokens(UUID uuid);


    @Transactional
    @Modifying
    @Query("""
            delete from Token t where t.expired = true or t.revoked = true
            """)
    void deleteExpiredAndRevokedTokens();

    @Transactional
    @Modifying
    @Query("""
        update Token t set t.expired = true where t.expiryDate < CURRENT_TIMESTAMP
        """)
    void markExpiredTokens();

    Token findByUserUuid(UUID uuid);
}
