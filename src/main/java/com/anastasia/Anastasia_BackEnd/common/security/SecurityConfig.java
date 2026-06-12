package com.anastasia.Anastasia_BackEnd.common.security;

import com.anastasia.Anastasia_BackEnd.common.filter.JwtFilter;
import com.anastasia.Anastasia_BackEnd.common.filter.SupportAccessGovernanceFilter;
import com.anastasia.Anastasia_BackEnd.common.filter.TenantFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

//    private final UserDetailsService userDetailsService;
    private final JwtFilter jwtFilter;
    private final TenantFilter tenantFilter;
    private final SupportAccessGovernanceFilter supportAccessGovernanceFilter;
    private final LogoutHandler logoutHandler;
    private final Environment environment;
    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;
    @Value("${app.security.oauth2-enabled:true}")
    private boolean oauth2Enabled;
    @Value("${app.security.allow-anonymous:false}")
    private boolean allowAnonymous;
    @Value("${app.security.public-docs-enabled:false}")
    private boolean publicDocsEnabled;
    @Value("${app.security.test-helper-secret:}")
    private String testHelperSecret;
    private static final String TEST_HELPER_SECRET_HEADER = "X-Test-Helper-Secret";
    private static final String[] WHITE_LIST_ENDPOINTS = {
            "/api/v1/auth/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/webhooks/stripe",
            "/api/v1/priests/register",
            "/api/v1/auth/platform-admin/register",
            "/api/v1/membership-cards/verify/**",
            "/ws/**",
            "/ws-sockjs/**"
    };
    private static final String[] DOCS_ENDPOINTS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v2/api-docs/**",
            "/v3/api-docs/**",
            "/api-docs/**",
            "/api/swagger-ui/**",
            "/webjars/**"
    };
    private static final String[] TEST_HELPER_ENDPOINTS = {
            "/api/v1/tenant/test/**",
            "/api/v1/auth/test/**",
            "/api/v1/test-utils/**",
            "/api/v1/test/**",
            "/test-utils/**"
    };
    private static final String[] CSRF_IGNORED_ENDPOINTS = {
            "/webhooks/stripe",
            "/api/v1/email/ses-events",
            "/api/v1/auth/login",
            "/api/v1/auth/sign-up",
            "/api/v1/auth/login/2fa/verify",
            "/api/v1/auth/resend-activation",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/public/contact-requests",
            "/api/v1/tenant/subscription",
            "/api/v1/tenant/verify-phone",
            "/api/v1/tenant/resend-phone-otp",
            "/api/v1/onboarding/email-verification/send-code",
            "/api/v1/onboarding/email-verification/verify-code",
            "/api/v1/onboarding/billing/sessions",
            "/api/v1/onboarding/billing/sessions/*/checkout",
            "/api/v1/onboarding/billing/sessions/*/finalize",
            "/api/v1/onboarding/billing/sessions/*/auto-login",
            "/api/v1/priests/register",
            "/api/v1/auth/platform-admin/register",
            "/ws/**",
            "/ws-sockjs/**",
            "/api/v1/tenant/test/**",
            "/api/v1/auth/test/**",
            "/api/v1/test-utils/**",
            "/api/v1/test/**",
            "/test-utils/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService,
            OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler,
            OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler
    ) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(CSRF_IGNORED_ENDPOINTS))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(request -> request
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(TEST_HELPER_ENDPOINTS).access(this::authorizeTestHelperRequest)
                        .requestMatchers(DOCS_ENDPOINTS).access((authentication, context) ->
                                new AuthorizationDecision(publicDocsEnabled))
                        .requestMatchers(WHITE_LIST_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/tenant/subscription",
                                "/api/v1/tenant/verify-phone",
                                "/api/v1/tenant/resend-phone-otp",
                                "/api/v1/public/contact-requests",
                                "/api/v1/onboarding/email-verification/send-code",
                                "/api/v1/onboarding/email-verification/verify-code",
                                "/api/v1/onboarding/billing/sessions",
                                "/api/v1/onboarding/billing/sessions/*/checkout",
                                "/api/v1/onboarding/billing/sessions/*/finalize",
                                "/api/v1/onboarding/billing/sessions/*/auto-login",
                                "/api/v1/priests/register",
                                "/api/v1/auth/platform-admin/register",
                                "/api/v1/email/ses-events"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/churches",
                                "/api/v1/churches/by-number/*",
                                "/api/v1/onboarding/billing/sessions/*",
                                "/api/v1/membership-cards/verify/*"
                        ).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .anonymous(allowAnonymous ? Customizer.withDefaults() : AbstractHttpConfigurer::disable) // controls if anonymous users are allowed
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                )
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .addLogoutHandler(logoutHandler)
                        .logoutSuccessHandler((request, response, authentication) -> {
                            SecurityContextHolder.clearContext();
                            response.setStatus(HttpServletResponse.SC_OK);
                        }))
        ;

        if (oauth2Enabled) {
            http.oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo.userService(oauth2UserService))
                    .successHandler(oauth2AuthenticationSuccessHandler)
                    .failureHandler(oauth2AuthenticationFailureHandler));
        }

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(tenantFilter, JwtFilter.class);
        http.addFilterAfter(supportAccessGovernanceFilter, TenantFilter.class);
        http.addFilterAfter(new CsrfCookieFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(TenantFilter tenantFilter) {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>(tenantFilter);
        registration.setEnabled(false);
        return registration;
    }

    private AuthorizationDecision authorizeTestHelperRequest(
            Supplier<org.springframework.security.core.Authentication> authentication,
            RequestAuthorizationContext context
    ) {
        String configuredSecret = testHelperSecret == null ? "" : testHelperSecret.trim();
        String providedSecret = context.getRequest().getHeader(TEST_HELPER_SECRET_HEADER);

        boolean allowed = StringUtils.hasText(configuredSecret)
                && StringUtils.hasText(providedSecret)
                && MessageDigest.isEqual(
                configuredSecret.getBytes(StandardCharsets.UTF_8),
                providedSecret.trim().getBytes(StandardCharsets.UTF_8)
        );
        return new AuthorizationDecision(allowed);
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
//        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        return new DefaultOAuth2UserService();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(resolveAllowedOrigins());

        // 2. Allow specific HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 3. Allow headers used by auth, tenancy, and idempotent billing session creation
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Support-Access-Session",
                "X-XSRF-TOKEN",
                "X-Requested-With",
                "X-Tenant-Id",
                "Idempotency-Key",
                "idempotency-key"
        ));

        // 4. Allow credentials (cookies, auth headers)
        configuration.setAllowCredentials(true);

        // 5. Apply this configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    private List<String> resolveAllowedOrigins() {
        Set<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (isLocalProfileActive()) {
            origins.add("http://localhost:4200");
            origins.add("http://127.0.0.1:4200");
            origins.add("http://192.168.1.79:4200");
        }

        return List.copyOf(origins);
    }

    private boolean isLocalProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "dev".equalsIgnoreCase(profile) || "test".equalsIgnoreCase(profile));
    }

    private static final class CsrfCookieFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }

}
