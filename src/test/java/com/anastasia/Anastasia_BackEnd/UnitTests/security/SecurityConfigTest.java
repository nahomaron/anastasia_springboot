package com.anastasia.Anastasia_BackEnd.UnitTests.security;

import com.anastasia.Anastasia_BackEnd.common.filter.JwtFilter;
import com.anastasia.Anastasia_BackEnd.common.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class SecurityConfigTest {

    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtFilter jwtFilter;
    @Mock
    private LogoutHandler logoutHandler;
    @Mock
    private Environment environment;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    void passwordEncoder_shouldBeBCrypt() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(encoder.matches("secret", encoder.encode("secret"))).isTrue();
    }

//    @Test
//    void authenticationProvider_shouldUseConfiguredServices() {
//        DaoAuthenticationProvider provider = (DaoAuthenticationProvider) securityConfig.authenticationProvider();
//
//        PasswordEncoder encoder = ReflectionTestUtils.invokeMethod(provider, "getPasswordEncoder");
//        Object uds = ReflectionTestUtils.getField(provider, "userDetailsService");
//
//        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
//        assertThat(uds).isEqualTo(userDetailsService);
//    }

    @Test
    void authenticationProvider_shouldUseConfiguredServices() {
        // Pass your (mocked) userDetailsService into the method
        DaoAuthenticationProvider provider = (DaoAuthenticationProvider) securityConfig.authenticationProvider(userDetailsService);

        PasswordEncoder encoder = (PasswordEncoder) ReflectionTestUtils.invokeMethod(provider, "getPasswordEncoder");
        Object uds = ReflectionTestUtils.getField(provider, "userDetailsService");

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(uds).isEqualTo(userDetailsService);
    }

    @Test
    void oauth2UserService_shouldReturnNonNullService() {
        assertThat(securityConfig.oauth2UserService()).isInstanceOf(DefaultOAuth2UserService.class);
    }

    @Test
    void corsConfigurationSource_shouldIncludeLocalOriginsForDevProfile() {
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins", "https://staging.anastasisapp.com");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).contains(
                "https://staging.anastasisapp.com",
                "http://localhost:4200",
                "http://127.0.0.1:4200"
        );
    }
}
