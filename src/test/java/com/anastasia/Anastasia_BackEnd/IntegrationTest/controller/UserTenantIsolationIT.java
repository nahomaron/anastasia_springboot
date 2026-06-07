package com.anastasia.Anastasia_BackEnd.IntegrationTest.controller;

import com.anastasia.Anastasia_BackEnd.AnastasiaBackEndApplication;
import com.anastasia.Anastasia_BackEnd.Api.config.PostgresTestContainer;
import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
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
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AnastasiaBackEndApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class UserTenantIsolationIT extends PostgresTestContainer {

    private static final String PASSWORD = TestDataUtil.TEST_PASSWORD;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ChurchRepository churchRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    private TenantEntity tenantOne;
    private TenantEntity tenantTwo;
    private UserEntity tenantOneAdmin;
    private UserEntity tenantOneUser;
    private UserEntity tenantTwoUser;
    private String tenantOneToken;

    @BeforeEach
    void setUp() throws Exception {
        Mockito.reset(rateLimiterService);
        when(rateLimiterService.tryConsume(anyString(), anyLong(), any(Duration.class))).thenReturn(true);

        tenantOne = createTenant("tenant-one");
        tenantTwo = createTenant("tenant-two");

        tenantOneAdmin = createUser(tenantOne, "PRIMARY_ADMIN", uniqueEmail("tenant.one.admin"));
        tenantOneUser = createUser(tenantOne, "USER", uniqueEmail("tenant.one.user"));
        createUser(tenantTwo, "PRIMARY_ADMIN", uniqueEmail("tenant.two.admin"));
        tenantTwoUser = createUser(tenantTwo, "USER", uniqueEmail("tenant.two.user"));

        tenantOneToken = login(tenantOneAdmin.getEmail());
    }

    @Test
    void listUsers_excludesUsersFromOtherTenants() throws Exception {
        mockMvc.perform(get("/api/v1/users/")
                        .header("Authorization", "Bearer " + tenantOneToken)
                        .header("X-Tenant-ID", tenantOne.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem(tenantOneAdmin.getUuid().toString())))
                .andExpect(jsonPath("$", hasItem(tenantOneUser.getUuid().toString())))
                .andExpect(jsonPath("$", not(hasItem(tenantTwoUser.getUuid().toString()))));
    }

    @Test
    void getUser_returnsNotFoundForUserFromOtherTenant() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}", tenantTwoUser.getUuid())
                        .header("Authorization", "Bearer " + tenantOneToken)
                        .header("X-Tenant-ID", tenantOne.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_returnsNotFoundForUserFromOtherTenant() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{userId}", tenantTwoUser.getUuid())
                        .with(csrf())
                        .header("Authorization", "Bearer " + tenantOneToken)
                        .header("X-Tenant-ID", tenantOne.getId()))
                .andExpect(status().isNotFound());

        assertTrue(userRepository.findById(tenantTwoUser.getUuid()).isPresent());
    }

    private TenantEntity createTenant(String slugPrefix) {
        TenantEntity tenant = TestDataUtil.createTestTenantEntity();
        tenant.setSlug(slugPrefix + "-" + UUID.randomUUID());
        tenant.setOwnerEmail(uniqueEmail(slugPrefix + ".owner"));
        tenant.setBillingEmail(uniqueEmail(slugPrefix + ".billing"));
        TenantEntity savedTenant = tenantRepository.save(tenant);

        ChurchEntity church = TestDataUtil.createTestChurchEntity(savedTenant);
        ChurchEntity savedChurch = churchRepository.save(church);
        savedTenant.assignChurch(savedChurch);
        return tenantRepository.save(savedTenant);
    }

    private UserEntity createUser(TenantEntity tenant, String roleName, String email) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not seeded: " + roleName));

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
}
