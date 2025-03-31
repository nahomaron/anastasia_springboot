package com.anastasia.Anastasia_BackEnd.util;


import com.anastasia.Anastasia_BackEnd.model.entity.auth.Role;
import com.anastasia.Anastasia_BackEnd.model.principal.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtUtil {

    private final SecretKey secretKey;

    private static final Long ACCESS_TOKEN_EXPIRATION_PERIOD = 1000L * 60 * 60 * 24;
    private static final Long REFRESH_TOKEN_EXPIRATION_PERIOD = 1000L * 60 * 60 * 24 * 7;

    @Autowired
    public JwtUtil() {
        String base64key = "1d8nU4bfO1i+6NDAQ3t5w9cI0D7+x1FFDrcc+P2NJGU=";
        byte[] keyByte = Base64.getDecoder().decode(base64key);
        secretKey = Keys.hmacShaKeyFor(keyByte);
    }


    public String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long EXPIRATION_PERIOD) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .signWith(secretKey)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_PERIOD))
                .compact();
    }

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(generateClaims(userDetails), userDetails, ACCESS_TOKEN_EXPIRATION_PERIOD);

    }


    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(generateClaims(userDetails), userDetails, REFRESH_TOKEN_EXPIRATION_PERIOD);
    }

    public Map<String, Object> generateClaims(UserDetails userDetails){
        if (!(userDetails instanceof UserPrincipal userPrincipal)) {
            throw new IllegalArgumentException("UserDetails is not an instance of UserPrincipal");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", userPrincipal.getTenantId()); // Store Tenant ID in JWT
        claims.put("roles", userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return claims;
    }

    public Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
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

}
