package com.anastasia.Anastasia_BackEnd.core.notification.config;

import com.anastasia.Anastasia_BackEnd.common.utils.JwtUtil;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.token.TokenType;
import com.anastasia.Anastasia_BackEnd.modules.users.service.CustomUserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailService userDetailService;
    private final TokenRepository tokenRepository;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String bearerToken = extractBearerToken(accessor);
        if (!StringUtils.hasText(bearerToken)) {
            throw new IllegalArgumentException("Missing Authorization token for WebSocket connection");
        }

        String username = jwtUtil.extractUsername(bearerToken);
        UserPrincipal userPrincipal = (UserPrincipal) userDetailService.loadUserByUsername(username);
        if (!jwtUtil.isTokenValid(bearerToken, userPrincipal)) {
            throw new IllegalArgumentException("Invalid Authorization token for WebSocket connection");
        }
        if (!isPersistedBearerTokenActive(bearerToken)) {
            throw new IllegalArgumentException("Revoked Authorization token for WebSocket connection");
        }

        validateTenantScope(accessor, bearerToken, userPrincipal);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities()
        );
        accessor.setUser(authentication);
        return message;
    }

    private boolean isPersistedBearerTokenActive(String bearerToken) {
        return tokenRepository.findActiveTokensByValueAndType(bearerToken, TokenType.BEARER).stream()
                .findFirst()
                .isPresent();
    }

    private String extractBearerToken(StompHeaderAccessor accessor) {
        String header = firstNonBlank(
                accessor.getFirstNativeHeader("Authorization"),
                accessor.getFirstNativeHeader("authorization")
        );
        if (!StringUtils.hasText(header)) {
            return null;
        }
        String normalized = header.trim();
        if (normalized.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            return normalized.substring(7);
        }
        return normalized;
    }

    private void validateTenantScope(StompHeaderAccessor accessor, String token, UserPrincipal principal) {
        String tokenTenant = jwtUtil.extractTenantId(token);
        String headerTenant = firstNonBlank(
                accessor.getFirstNativeHeader("X-Tenant-Id"),
                accessor.getFirstNativeHeader("X-Tenant-ID"),
                accessor.getFirstNativeHeader("x-tenant-id")
        );

        if (StringUtils.hasText(headerTenant) && StringUtils.hasText(tokenTenant) && !headerTenant.equals(tokenTenant)) {
            throw new IllegalArgumentException("Tenant mismatch between WebSocket header and token");
        }

        if (StringUtils.hasText(tokenTenant) && principal.getTenantId() != null) {
            UUID tokenTenantId = UUID.fromString(tokenTenant);
            if (!tokenTenantId.equals(principal.getTenantId())) {
                throw new IllegalArgumentException("Authenticated user does not belong to token tenant");
            }
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
