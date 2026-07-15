package com.anastasia.Anastasia_BackEnd.IntegrationTest.controller;

import com.anastasia.Anastasia_BackEnd.AnastasiaBackEndApplication;
import com.anastasia.Anastasia_BackEnd.Api.config.PostgresTestContainer;
import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventStatus;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventType;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventVisibilityType;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationDeliveryStatus;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.core.notification.repository.NotificationRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AnastasiaBackEndApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class MobileControllerTenantIsolationIT extends PostgresTestContainer {

    private static final String PASSWORD = TestDataUtil.TEST_PASSWORD;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ChurchRepository churchRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private RateLimiterService rateLimiterService;

    private TenantEntity tenantOne;
    private TenantEntity tenantTwo;
    private ChurchEntity tenantOneChurch;
    private ChurchEntity tenantTwoChurch;
    private UserEntity tenantOneUser;
    private UserEntity tenantTwoUser;
    private String tenantOneToken;

    @BeforeEach
    void setUp() throws Exception {
        Mockito.reset(rateLimiterService);
        when(rateLimiterService.tryConsume(anyString(), anyLong(), any(Duration.class))).thenReturn(true);

        tenantOne = createTenant("tenant-one");
        tenantTwo = createTenant("tenant-two");
        tenantOneChurch = createChurch(tenantOne);
        tenantTwoChurch = createChurch(tenantTwo);

        tenantOneUser = createUser(tenantOne, uniqueEmail("tenant.one.user"));
        tenantTwoUser = createUser(tenantTwo, uniqueEmail("tenant.two.user"));
        tenantOneToken = login(tenantOneUser.getEmail());
    }

    @Test
    void memberDetail_doesNotExposeOtherTenantMember() throws Exception {
        Adult_MemberEntity otherTenantMember = memberRepository.save(TestDataUtil.createTestMember(tenantTwoChurch));

        mockMvc.perform(get("/api/v1/mobile/members/{memberId}", otherTenantMember.getId())
                        .header("Authorization", "Bearer " + tenantOneToken)
                        .header("X-Tenant-ID", tenantOne.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void eventDetail_doesNotExposeOtherTenantEvent() throws Exception {
        EventEntity otherTenantEvent = eventRepository.save(EventEntity.builder()
                .tenantId(tenantTwo.getId())
                .church(tenantTwoChurch)
                .title("Cross Tenant Event")
                .description("Should not leak")
                .location("Main Sanctuary")
                .startAt(Instant.parse("2026-07-05T08:00:00Z"))
                .endAt(Instant.parse("2026-07-05T10:00:00Z"))
                .timezone("UTC")
                .allDay(false)
                .status(EventStatus.SCHEDULED)
                .type(EventType.LITURGY)
                .visibility(EventVisibilityType.ALL)
                .invitedEmails(Collections.emptySet())
                .build());

        mockMvc.perform(get("/api/v1/mobile/events/{eventId}", otherTenantEvent.getEventId())
                        .header("Authorization", "Bearer " + tenantOneToken)
                        .header("X-Tenant-ID", tenantOne.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void session_usesAuthenticatedTenantContext() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/session")
                        .header("Authorization", "Bearer " + tenantOneToken)
                        .header("X-Tenant-ID", tenantOne.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant.id").value(tenantOne.getId().toString()));
    }

    @Test
    void notifications_includesSystemAndTenantScopesForMatchingTenant() throws Exception {
        notificationRepository.save(createNotification(tenantOneUser.getUuid(), null, "System notice", NotificationType.NOTIFICATION));
        notificationRepository.save(createNotification(tenantOneUser.getUuid(), tenantOne, "Tenant notice", NotificationType.EVENT_REMINDER));

        mockMvc.perform(get("/api/v1/mobile/notifications")
                        .header("Authorization", "Bearer " + tenantOneToken)
                        .header("X-Tenant-ID", tenantOne.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[?(@.scope=='SYSTEM')]", hasSize(1)))
                .andExpect(jsonPath("$.items[?(@.scope=='TENANT')]", hasSize(1)))
                .andExpect(jsonPath("$.unreadCount").value(2));
    }

    @Test
    void notifications_rejectsCrossTenantTenantNotifications() throws Exception {
        notificationRepository.save(createNotification(tenantOneUser.getUuid(), tenantOne, "Tenant notice", NotificationType.EVENT_REMINDER));

        mockMvc.perform(get("/api/v1/mobile/notifications")
                        .header("Authorization", "Bearer " + tenantOneToken)
                        .header("X-Tenant-ID", tenantTwo.getId()))
                .andExpect(status().isForbidden());
    }

    private TenantEntity createTenant(String slugPrefix) {
        TenantEntity tenant = TestDataUtil.createTestTenantEntity();
        tenant.setSlug(slugPrefix + "-" + UUID.randomUUID());
        tenant.setOwnerEmail(uniqueEmail(slugPrefix + ".owner"));
        tenant.setBillingEmail(uniqueEmail(slugPrefix + ".billing"));
        return tenantRepository.save(tenant);
    }

    private ChurchEntity createChurch(TenantEntity tenant) {
        ChurchEntity church = TestDataUtil.createTestChurchEntity(tenant);
        ChurchEntity saved = churchRepository.save(church);
        tenant.assignChurch(saved);
        tenantRepository.save(tenant);
        return saved;
    }

    private UserEntity createUser(TenantEntity tenant, String email) {
        var role = roleRepository.findByRoleName("PRIMARY_ADMIN")
                .orElseThrow(() -> new IllegalStateException("Role not seeded: PRIMARY_ADMIN"));

        UserEntity user = TestDataUtil.createTestUserEntityA();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRoles(Set.of(role));
        user.assignTenant(tenant);
        user.setEmailVerifiedAt(Instant.now());
        user.setVerified(true);
        return userRepository.save(user);
    }

    private String login(String email) throws Exception {
        AuthenticationRequest request = new AuthenticationRequest(email, PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("accessToken").asText();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID() + "@example.com";
    }

    private NotificationEntity createNotification(UUID userId, TenantEntity notificationTenant, String title, NotificationType type) {
        NotificationEntity entity = new NotificationEntity();
        entity.setRecipientEmail("notifications@example.com");
        entity.setRecipientUserId(userId);
        entity.setTitle(title);
        entity.setMessage(title + " body");
        entity.setChannel(NotificationChannelType.IN_APP);
        entity.setType(type);
        entity.setDeliveryStatus(NotificationDeliveryStatus.SENT);
        entity.setSentAt(Instant.now());
        entity.setTenant(notificationTenant);
        return entity;
    }
}
