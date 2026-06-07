package com.anastasia.Anastasia_BackEnd.UnitTests.core.notification.config;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.utils.JwtUtil;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import com.anastasia.Anastasia_BackEnd.core.auth.token.TokenType;
import com.anastasia.Anastasia_BackEnd.core.notification.config.WebSocketAuthChannelInterceptor;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.service.CustomUserDetailService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailService userDetailService;

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private WebSocketAuthChannelInterceptor interceptor;

    @Test
    void preSend_shouldRejectRevokedBearerTokens() {
        Message<?> connectMessage = connectMessage("stale-token", null);
        UserPrincipal principal = principal("member@example.com", null);

        when(jwtUtil.extractUsername("stale-token")).thenReturn("member@example.com");
        when(jwtUtil.extractJwtId("stale-token")).thenReturn("jwt-stale");
        when(userDetailService.loadUserByUsername("member@example.com")).thenReturn(principal);
        when(jwtUtil.isTokenValid("stale-token", principal)).thenReturn(true);
        when(tokenRepository.findActiveTokensByJwtIdAndType("jwt-stale", TokenType.BEARER)).thenReturn(List.of());

        assertThatThrownBy(() -> interceptor.preSend(connectMessage, mock(MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Revoked Authorization token");
    }

    @Test
    void preSend_shouldAuthenticateWhenBearerTokenIsStillActive() {
        UUID tenantId = UUID.randomUUID();
        Message<?> connectMessage = connectMessage("active-token", tenantId.toString());
        UserPrincipal principal = principal("member@example.com", tenantId);

        when(jwtUtil.extractUsername("active-token")).thenReturn("member@example.com");
        when(jwtUtil.extractJwtId("active-token")).thenReturn("jwt-active");
        when(userDetailService.loadUserByUsername("member@example.com")).thenReturn(principal);
        when(jwtUtil.isTokenValid("active-token", principal)).thenReturn(true);
        when(jwtUtil.extractTenantId("active-token")).thenReturn(tenantId.toString());
        when(tokenRepository.findActiveTokensByJwtIdAndType("jwt-active", TokenType.BEARER))
                .thenReturn(List.of(Token.builder().jwtId("jwt-active").tokenType(TokenType.BEARER).build()));

        Message<?> authenticated = interceptor.preSend(connectMessage, mock(MessageChannel.class));
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(authenticated);

        assertThat(accessor.getUser()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(((Authentication) accessor.getUser()).getPrincipal()).isEqualTo(principal);
    }

    private Message<?> connectMessage(String token, String tenantId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        if (tenantId != null) {
            accessor.setNativeHeader("X-Tenant-Id", tenantId);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private UserPrincipal principal(String email, UUID tenantId) {
        UserEntity user = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email(email)
                .password("secret")
                .status(UserStatus.ACTIVE)
                .roles(Set.of())
                .build();
        if (tenantId != null) {
            user.setTenant(com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity.builder()
                    .id(tenantId)
                    .build());
        }
        return new UserPrincipal(user);
    }
}
