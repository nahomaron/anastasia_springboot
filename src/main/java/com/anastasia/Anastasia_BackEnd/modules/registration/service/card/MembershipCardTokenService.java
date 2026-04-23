package com.anastasia.Anastasia_BackEnd.modules.registration.service.card;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class MembershipCardTokenService {

    private final SecretKey secretKey;

    public MembershipCardTokenService(@Value("${jwt.secret:}") String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("jwt.secret must be configured for membership card tokens");
        }
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generate(Long cardId, UUID tenantId, String membershipNumber, LocalDate expiresAt) {
        Date expirationDate = Date.from(expiresAt.plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant());

        return Jwts.builder()
                .subject("membership-card")
                .claim("cid", cardId)
                .claim("tid", tenantId.toString())
                .claim("mno", membershipNumber)
                .issuedAt(new Date())
                .expiration(expirationDate)
                .signWith(secretKey)
                .compact();
    }

    public ParsedMembershipCardToken parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .clockSkewSeconds(5)
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            Claims claims = jws.getPayload();
            Long cardId = claims.get("cid", Long.class);
            String tenantId = claims.get("tid", String.class);
            String membershipNumber = claims.get("mno", String.class);

            if (cardId == null || tenantId == null || membershipNumber == null) {
                throw new IllegalArgumentException("Invalid membership card token claims");
            }

            return new ParsedMembershipCardToken(cardId, UUID.fromString(tenantId), membershipNumber);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid membership card token", ex);
        }
    }

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash membership card token", ex);
        }
    }

    public record ParsedMembershipCardToken(Long cardId, UUID tenantId, String membershipNumber) {
    }
}
