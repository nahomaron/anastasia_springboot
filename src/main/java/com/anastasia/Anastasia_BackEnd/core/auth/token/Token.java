package com.anastasia.Anastasia_BackEnd.core.auth.token;

import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tokens")
public class Token {

    @Id
    @SequenceGenerator(name = "token_seq", sequenceName = "tokens_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "token_seq")
    private int id;

    @Column(length = 500)
    private String token;

    @Column(length = 64)
    private String jwtId;

    @Column(length = 64)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    private Instant createdAt;

    private Instant expiresAt;

    private Instant validatedAt;

    private Instant revokedAt;

    private Instant expiredAt;

    private Instant deletedAt;

    private boolean expired;

    private boolean revoked;

    @Version
    @Column(nullable = false)
    private long version;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    public Instant getExpiryDate() {
        return expiresAt;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiresAt = expiryDate;
    }

}
