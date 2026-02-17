package com.anastasia.Anastasia_BackEnd.core.auth.token;

import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tokens")
public class Token {

    @Id
    @GeneratedValue
    private int id;

    @Column(length = 500)
    private String token;

    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    private LocalDateTime validatedAt;

    private Instant expiryDate;

    private boolean expired;

    private boolean revoked;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

}

