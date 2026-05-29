package com.anastasia.Anastasia_BackEnd.IntegrationTest.controller;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.Api.config.PostgresTestContainer;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.TenantService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import jakarta.transaction.Transactional;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Epic("Integration Tests")
@Feature("Internal Layer")
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
public class TenantControllerIT extends PostgresTestContainer {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private TenantService tenantService;

    private TenantDTO tenantDTO;
    private TenantEntity savedTenant;

    @BeforeEach
    void setup() {
        tenantDTO = TestDataUtil.createTestTenantDTO();
        TenantDTO existingTenantDTO = TestDataUtil.createTestTenantDTO();
        savedTenant = tenantRepository.save(tenantService.convertTenantToEntity(existingTenantDTO));
    }

    @Test
    void testSubscribeTenant_success() throws Exception {
        mockMvc.perform(post("/api/v1/tenant/subscription")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tenantDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    void testSubscribeTenant_passwordMismatch() throws Exception {
        tenantDTO.setConfirmPassword("wrongPassword");

        mockMvc.perform(post("/api/v1/tenant/subscription")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tenantDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Passwords do not match."));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_TENANTS")
    void testListOfTenants() throws Exception {
        mockMvc.perform(get("/api/v1/tenant")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(authorities = "MANAGE_TENANTS")
    void testGetTenant_found() throws Exception {
        mockMvc.perform(get("/api/v1/tenant/{tenantId}", savedTenant.getId()))
                .andExpect(status().isFound());
    }

    @Test
    @WithMockUser(authorities = "MANAGE_TENANTS")
    void testGetTenant_notFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/tenant/{tenantId}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "OWN_SUBSCRIPTION")
    void testUnsubscribeTenant_success() throws Exception {
        mockMvc.perform(post("/api/v1/tenant/unsubscribe/{tenantId}", savedTenant.getId())
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}
