package com.anastasia.Anastasia_BackEnd.UnitTests.registration.onboarding;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantOnboardingSessionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingSessionAccessService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class OnboardingSessionAccessServiceTest {

    @Mock
    private LocalizedMessageService messageService;

    private OnboardingSessionAccessService accessService;

    @BeforeEach
    void setUp() {
        accessService = new OnboardingSessionAccessService(messageService);
        when(messageService.get(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    void issuedTokenGrantsAccessToSession() {
        TenantOnboardingSessionEntity session = TenantOnboardingSessionEntity.builder()
                .expiresAt(Instant.now().plusSeconds(600))
                .build();

        String token = accessService.issueAccessToken(session);

        assertThat(token).isNotBlank();
        assertThat(session.getAccessTokenHash()).isNotBlank();
        accessService.assertSessionAccess(session, token);
    }

    @Test
    void assertSessionAccessRejectsMissingOrInvalidToken() {
        TenantOnboardingSessionEntity session = TenantOnboardingSessionEntity.builder()
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        accessService.issueAccessToken(session);

        assertThatThrownBy(() -> accessService.assertSessionAccess(session, null))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> accessService.assertSessionAccess(session, "wrong-token"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void extractAccessTokenPrefersHeaderThenCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(OnboardingSessionAccessService.ONBOARDING_ACCESS_COOKIE_NAME, "cookie-token"));
        request.addHeader(OnboardingSessionAccessService.ONBOARDING_ACCESS_HEADER_NAME, "header-token");

        assertThat(accessService.extractAccessToken(request)).contains("header-token");

        MockHttpServletRequest cookieOnly = new MockHttpServletRequest();
        cookieOnly.setCookies(new Cookie(OnboardingSessionAccessService.ONBOARDING_ACCESS_COOKIE_NAME, "cookie-token"));
        assertThat(accessService.extractAccessToken(cookieOnly)).contains("cookie-token");
    }

    @Test
    void addAccessTokenCookieUsesOnboardingCookieName() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ReflectionTestUtils.setField(accessService, "secureCookie", true);
        ReflectionTestUtils.setField(accessService, "sameSite", "None");

        accessService.addAccessTokenCookie(response, "token-value", Instant.now().plusSeconds(600));

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).contains(OnboardingSessionAccessService.ONBOARDING_ACCESS_COOKIE_NAME + "=token-value");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("SameSite=None");
    }

    @Test
    void insecureCookieNeverEmitsSameSiteNone() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ReflectionTestUtils.setField(accessService, "secureCookie", false);
        ReflectionTestUtils.setField(accessService, "sameSite", "None");

        accessService.addAccessTokenCookie(response, "token-value", Instant.now().plusSeconds(600));

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).doesNotContain("Secure");
        assertThat(setCookie).contains("SameSite=Lax");
    }
}
