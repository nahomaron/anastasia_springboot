package com.anastasia.Anastasia_BackEnd.IntegrationTest.controller;

import com.anastasia.Anastasia_BackEnd.AnastasiaBackEndApplication;
import com.anastasia.Anastasia_BackEnd.Api.config.PostgresTestContainer;
import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import com.anastasia.Anastasia_BackEnd.core.auth.audit.PlatformAdminBootstrapAuditEvent;
import com.anastasia.Anastasia_BackEnd.core.auth.audit.PlatformAdminBootstrapAuditOutcome;
import com.anastasia.Anastasia_BackEnd.core.auth.audit.PlatformAdminBootstrapAuditRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.PlatformAdminRegistrationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.service.PlatformAdminRegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AnastasiaBackEndApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PlatformAdminRegistrationControllerIT extends PostgresTestContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformAdminBootstrapAuditRepository auditRepository;

    @Autowired
    private PlatformAdminRegistrationService platformAdminRegistrationService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        Mockito.reset(rateLimiterService);
        when(rateLimiterService.tryConsume(anyString(), anyLong(), any(Duration.class))).thenReturn(true);
        ReflectionTestUtils.setField(platformAdminRegistrationService, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(platformAdminRegistrationService, "configuredSecret", "dev-secret");
    }

    @Test
    void registerPlatformAdmin_bootstrapSucceedsAndAudits() throws Exception {
        mockMvc.perform(post("/api/v1/auth/platform-admin/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Platform-Admin-Secret", "dev-secret")
                        .header("User-Agent", "MockMvc-Test")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .content(objectMapper.writeValueAsString(request("bootstrap@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Platform admin bootstrap completed successfully"));

        assertThat(userRepository.findByEmailIgnoreCase("bootstrap@example.com")).isPresent();

        PlatformAdminBootstrapAuditEvent event = latestAuditEvent();
        assertThat(event.getOutcome()).isEqualTo(PlatformAdminBootstrapAuditOutcome.SUCCESS);
        assertThat(event.getAttemptedEmail()).isEqualTo("bootstrap@example.com");
        assertThat(event.getIpAddress()).isEqualTo("203.0.113.10");
        assertThat(event.getUserAgent()).isEqualTo("MockMvc-Test");
        assertThat(event.getCreatedUserId()).isNotNull();
    }

    @Test
    void registerPlatformAdmin_secondBootstrapIsRejectedAndAudited() throws Exception {
        mockMvc.perform(post("/api/v1/auth/platform-admin/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Platform-Admin-Secret", "dev-secret")
                        .content(objectMapper.writeValueAsString(request("first-bootstrap@example.com"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/platform-admin/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Platform-Admin-Secret", "dev-secret")
                        .content(objectMapper.writeValueAsString(request("second-bootstrap@example.com"))))
                .andExpect(status().isConflict());

        assertThat(userRepository.findByEmailIgnoreCase("second-bootstrap@example.com")).isEmpty();

        PlatformAdminBootstrapAuditEvent event = latestAuditEvent();
        assertThat(event.getOutcome()).isEqualTo(PlatformAdminBootstrapAuditOutcome.BOOTSTRAP_ALREADY_COMPLETED);
        assertThat(event.getAttemptedEmail()).isEqualTo("second-bootstrap@example.com");
    }

    @Test
    void registerPlatformAdmin_invalidSecretIsRejectedAndAudited() throws Exception {
        mockMvc.perform(post("/api/v1/auth/platform-admin/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Platform-Admin-Secret", "wrong-secret")
                        .content(objectMapper.writeValueAsString(request("wrong-secret@example.com"))))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findByEmailIgnoreCase("wrong-secret@example.com")).isEmpty();

        PlatformAdminBootstrapAuditEvent event = latestAuditEvent();
        assertThat(event.getOutcome()).isEqualTo(PlatformAdminBootstrapAuditOutcome.INVALID_SECRET);
        assertThat(event.getAttemptedEmail()).isEqualTo("wrong-secret@example.com");
    }

    @Test
    void registerPlatformAdmin_rateLimitedRequestIsRejectedAndAudited() throws Exception {
        when(rateLimiterService.tryConsume(anyString(), anyLong(), any(Duration.class))).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/platform-admin/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Platform-Admin-Secret", "dev-secret")
                        .content(objectMapper.writeValueAsString(request("rate-limited@example.com"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many requests, try again later"));

        assertThat(userRepository.findByEmailIgnoreCase("rate-limited@example.com")).isEmpty();

        PlatformAdminBootstrapAuditEvent event = latestAuditEvent();
        assertThat(event.getOutcome()).isEqualTo(PlatformAdminBootstrapAuditOutcome.RATE_LIMITED);
        assertThat(event.getAttemptedEmail()).isEqualTo("rate-limited@example.com");
    }

    @Test
    void registerPlatformAdmin_bootstrapDisabledIsRejectedAndAudited() throws Exception {
        ReflectionTestUtils.setField(platformAdminRegistrationService, "bootstrapEnabled", false);

        mockMvc.perform(post("/api/v1/auth/platform-admin/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Platform-Admin-Secret", "dev-secret")
                        .content(objectMapper.writeValueAsString(request("disabled@example.com"))))
                .andExpect(status().isConflict());

        assertThat(userRepository.findByEmailIgnoreCase("disabled@example.com")).isEmpty();

        PlatformAdminBootstrapAuditEvent event = latestAuditEvent();
        assertThat(event.getOutcome()).isEqualTo(PlatformAdminBootstrapAuditOutcome.BOOTSTRAP_DISABLED);
        assertThat(event.getAttemptedEmail()).isEqualTo("disabled@example.com");
    }

    private PlatformAdminRegistrationRequest request(String email) {
        PlatformAdminRegistrationRequest request = new PlatformAdminRegistrationRequest();
        request.setFullName("Bootstrap Admin");
        request.setEmail(email);
        request.setPassword("StrongPass123!");
        return request;
    }

    private PlatformAdminBootstrapAuditEvent latestAuditEvent() {
        return auditRepository.findAll().stream()
                .max(Comparator.comparing(PlatformAdminBootstrapAuditEvent::getId))
                .orElseThrow();
    }
}
