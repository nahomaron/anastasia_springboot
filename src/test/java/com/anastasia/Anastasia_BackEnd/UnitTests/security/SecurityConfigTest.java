package com.anastasia.Anastasia_BackEnd.UnitTests.security;

import com.anastasia.Anastasia_BackEnd.filter.JwtFilter;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtFilter jwtFilter;
    @Mock
    private LogoutHandler logoutHandler;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    void passwordEncoder_shouldBeBCrypt() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(encoder.matches("secret", encoder.encode("secret"))).isTrue();
    }

    @Test
    void authenticationProvider_shouldUseConfiguredServices() {
        DaoAuthenticationProvider provider = (DaoAuthenticationProvider) securityConfig.authenticationProvider();

        PasswordEncoder encoder = ReflectionTestUtils.invokeMethod(provider, "getPasswordEncoder");
        Object uds = ReflectionTestUtils.getField(provider, "userDetailsService");

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(uds).isEqualTo(userDetailsService);
    }

    @Test
    void oauth2UserService_shouldReturnNonNullService() {
        UserRepository userRepository = mock(UserRepository.class);

        assertThat(securityConfig.oauth2UserService(userRepository)).isNotNull();
    }
}
