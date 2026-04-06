package com.anastasia.Anastasia_BackEnd.core.auth.repository;

import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import com.anastasia.Anastasia_BackEnd.core.auth.token.TokenType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer> {

    Optional<Token> findTopByTokenOrderByIdDesc(String token);

    @Query("""
            select t from Token t
            where t.token = :token
              and t.tokenType = :tokenType
              and t.expired = false
              and t.revoked = false
              and t.deletedAt is null
            order by t.id desc
            """)
    List<Token> findActiveTokensByValueAndType(@Param("token") String token, @Param("tokenType") TokenType tokenType);

    @Query("""
            select t from Token t inner join UserEntity u on t.user.id = u.uuid
            where u.uuid = :uuid and t.expired = false and t.revoked = false and t.deletedAt is null
            """)
    List<Token> findAllValidUserTokens(@Param("uuid") UUID uuid);

    @Query("""
        SELECT t FROM Token t JOIN UserEntity u ON t.user.uuid = u.uuid
        WHERE u.uuid = :userId AND t.tokenType = :tokenType AND t.expired = false AND t.revoked = false AND t.deletedAt is null
    """)
    List<Token> findAllValidTokensByUser(@Param("userId") UUID uuid, @Param("tokenType") TokenType tokenType);


    @Transactional
    @Modifying
    @Query("""
            delete from Token t where t.deletedAt is not null or (t.expired = true and t.revoked = true)
            """)
    void deleteExpiredAndRevokedTokens();

    @Transactional
    @Modifying
    @Query("""
        update Token t set t.expired = true, t.expiredAt = :now
        where t.expiresAt < :now and t.expired = false
        """)
    void markExpiredTokens(@Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("""
        update Token t
        set t.expired = true,
            t.revoked = true,
            t.expiredAt = :now,
            t.revokedAt = :now
        where t.id = :tokenId
          and t.user.uuid = :userId
          and t.deletedAt is null
          and (t.expired = false or t.revoked = false or t.expiredAt is null or t.revokedAt is null)
        """)
    int revokeTokenByIdAndUserUuid(@Param("tokenId") Integer tokenId, @Param("userId") UUID userId, @Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("""
        update Token t
        set t.expired = true,
            t.revoked = true,
            t.expiredAt = :now,
            t.revokedAt = :now
        where t.token = :token
          and t.deletedAt is null
          and (t.expired = false or t.revoked = false or t.expiredAt is null or t.revokedAt is null)
        """)
    int revokeTokenByValue(@Param("token") String token, @Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("""
        update Token t
        set t.expired = true,
            t.revoked = true,
            t.expiredAt = :now,
            t.revokedAt = :now
        where t.user.uuid = :userId
          and t.expired = false
          and t.revoked = false
          and t.deletedAt is null
        """)
    int revokeAllActiveTokensByUserUuid(@Param("userId") UUID userId, @Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("""
        update Token t
        set t.expired = true,
            t.revoked = true,
            t.expiredAt = :now,
            t.revokedAt = :now
        where t.user.uuid = :userId
          and t.sessionId = :sessionId
          and t.expired = false
          and t.revoked = false
          and t.deletedAt is null
        """)
    int revokeAllActiveTokensByUserUuidAndSessionId(@Param("userId") UUID userId, @Param("sessionId") String sessionId, @Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("""
        update Token t
        set t.expired = true,
            t.revoked = true,
            t.expiredAt = :now,
            t.revokedAt = :now
        where t.user.uuid = :userId
          and t.sessionId = :sessionId
          and t.tokenType = :tokenType
          and t.expired = false
          and t.revoked = false
          and t.deletedAt is null
        """)
    int revokeAllActiveTokensByUserUuidAndSessionIdAndType(@Param("userId") UUID userId,
                                                           @Param("sessionId") String sessionId,
                                                           @Param("tokenType") TokenType tokenType,
                                                           @Param("now") Instant now);

    Token findByUserUuid(UUID uuid);

    @Query("""
        SELECT t FROM Token t
        WHERE t.user.uuid = :userId AND t.tokenType = :tokenType
        ORDER BY t.id DESC
    """)
    List<Token> findByUserUuidAndTokenTypeOrderByIdDesc(@Param("userId") UUID userId, @Param("tokenType") TokenType tokenType);

    @Query("""
        SELECT t FROM Token t
        WHERE t.user.uuid = :userId
          AND t.expired = false
          AND t.revoked = false
          AND t.deletedAt is null
        ORDER BY t.id DESC
    """)
    List<Token> findAllActiveTokensByUserUuid(@Param("userId") UUID userId);

    @Query("""
        SELECT t FROM Token t
        WHERE t.user.uuid = :userId
          AND t.sessionId = :sessionId
          AND t.expired = false
          AND t.revoked = false
          AND t.deletedAt is null
        ORDER BY t.id DESC
    """)
    List<Token> findAllActiveTokensByUserUuidAndSessionId(@Param("userId") UUID userId, @Param("sessionId") String sessionId);

    @Query("""
        SELECT t FROM Token t
        WHERE t.id = :tokenId AND t.user.uuid = :userId
    """)
    Optional<Token> findByIdAndUserUuid(@Param("tokenId") Integer tokenId, @Param("userId") UUID userId);

    Optional<Token> findTopByUserEmailIgnoreCaseAndTokenTypeAndDeletedAtIsNullOrderByIdDesc(String email, TokenType tokenType);

    @Transactional
    @Modifying
    @Query("delete from Token t where t.user.uuid = :userId")
    void deleteAllByUserUuid(@Param("userId") UUID userId);


}
