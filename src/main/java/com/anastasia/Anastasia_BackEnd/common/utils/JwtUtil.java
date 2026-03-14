package com.anastasia.Anastasia_BackEnd.common.utils;


import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtUtil {

    private final SecretKey signingKey;
    private final List<SecretKey> verificationKeys;

    private static final Long ACCESS_TOKEN_EXPIRATION_PERIOD = 1000L * 60 * 60 * 24;
    private static final Long REFRESH_TOKEN_EXPIRATION_PERIOD = 1000L * 60 * 60 * 24 * 7;

    public JwtUtil(
            @Value("${app.auth.jwt-current-secret}") String currentBase64Key,
            @Value("${app.auth.jwt-previous-secret:}") String previousBase64Key
    ) {
        this(currentBase64Key, previousBase64Key, "app.auth.jwt-current-secret", "app.auth.jwt-previous-secret");
    }

    public JwtUtil(String currentBase64Key) {
        this(currentBase64Key, null, "jwt current secret", "jwt previous secret");
    }

    private JwtUtil(String currentBase64Key, String previousBase64Key, String currentPropertyName, String previousPropertyName) {
        this.signingKey = decodeSecretKey(currentBase64Key, currentPropertyName);
        this.verificationKeys = buildVerificationKeys(signingKey, previousBase64Key, previousPropertyName);
    }

    public static JwtUtil forSecrets(String currentBase64Key, String previousBase64Key) {
        return new JwtUtil(currentBase64Key, previousBase64Key, "jwt current secret", "jwt previous secret");
    }


    public String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long EXPIRATION_PERIOD) {
        String jwtId = UUID.randomUUID().toString();
        return buildToken(extraClaims, userDetails, EXPIRATION_PERIOD, jwtId);
    }

    public String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long EXPIRATION_PERIOD, String jwtId) {
        return Jwts.builder()
                .claims(extraClaims)
                .id(jwtId)
                .subject(userDetails.getUsername())
                .signWith(signingKey)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_PERIOD))
                .compact();
    }

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(generateClaims(userDetails), userDetails, ACCESS_TOKEN_EXPIRATION_PERIOD);

    }

    public String generateAccessToken(UserDetails userDetails, String sessionId, String jwtId) {
        Map<String, Object> claims = generateClaims(userDetails);
        claims.put("sessionId", sessionId);
        return buildToken(claims, userDetails, ACCESS_TOKEN_EXPIRATION_PERIOD, jwtId);
    }


    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(generateClaims(userDetails), userDetails, REFRESH_TOKEN_EXPIRATION_PERIOD);
    }

    public String generateRefreshToken(UserDetails userDetails, String sessionId, String jwtId) {
        Map<String, Object> claims = generateClaims(userDetails);
        claims.put("sessionId", sessionId);
        return buildToken(claims, userDetails, REFRESH_TOKEN_EXPIRATION_PERIOD, jwtId);
    }

    public Map<String, Object> generateClaims(UserDetails userDetails){
        if (!(userDetails instanceof UserPrincipal userPrincipal)) {
            throw new IllegalArgumentException("UserDetails is not an instance of UserPrincipal");
        }

        Map<String, Object> claims = new HashMap<>();
        if (userPrincipal.getTenantId() != null) {
            claims.put("tenantId", userPrincipal.getTenantId().toString());
        }
        claims.put("roles", userPrincipal.getRoles().stream()
                .map(role -> {
                    String roleName = role.getRoleName();
                    return roleName != null && roleName.startsWith("ROLE_")
                            ? roleName
                            : "ROLE_" + roleName;
                })
                .collect(Collectors.toList()));

        return claims;
    }


    public Claims extractAllClaims(String token){
        JwtException lastJwtException = null;
        IllegalArgumentException lastArgumentException = null;

        for (SecretKey verificationKey : verificationKeys) {
            try {
                return Jwts.parser()
                        .clockSkewSeconds(5)
                        .verifyWith(verificationKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (JwtException ex) {
                lastJwtException = ex;
            } catch (IllegalArgumentException ex) {
                lastArgumentException = ex;
            }
        }

        if (lastJwtException != null) {
            throw lastJwtException;
        }
        if (lastArgumentException != null) {
            throw lastArgumentException;
        }
        throw new IllegalStateException("No JWT verification keys configured");
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimResolver){
        Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenValid(String token, UserDetails userDetails){
        String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    public String extractTenantId(String token) {
        return extractClaim(token, claims -> claims.get("tenantId", String.class));
    }

    public String extractJwtId(String token) {
        return extractClaim(token, Claims::getId);
    }

    public String extractSessionId(String token) {
        return extractClaim(token, claims -> claims.get("sessionId", String.class));
    }

    public List<String> extractRoles(String token){
        return extractClaim(token, claims -> (List<String>) claims.get("roles"));

    }

    public static String generateBase64Secret() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    private static SecretKey decodeSecretKey(String base64Key, String propertyName) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(propertyName + " must be configured");
        }

        byte[] keyByte;
        try {
            keyByte = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(propertyName + " must be a valid Base64-encoded key", ex);
        }

        if (keyByte.length < 32) {
            throw new IllegalStateException(propertyName + " must decode to at least 32 bytes");
        }

        return Keys.hmacShaKeyFor(keyByte);
    }

    private static List<SecretKey> buildVerificationKeys(SecretKey currentKey, String previousBase64Key, String previousPropertyName) {
        List<SecretKey> keys = new ArrayList<>();
        keys.add(currentKey);
        if (previousBase64Key != null && !previousBase64Key.isBlank()) {
            keys.add(decodeSecretKey(previousBase64Key, previousPropertyName));
        }
        return List.copyOf(keys);
    }

}
