package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.service.registration.TenantService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.anastasia.Anastasia_BackEnd.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TenantControllerIT {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private TenantService tenantService;

    private TenantDTO tenantDTO;
    private TenantEntity savedTenant;

    @BeforeEach
    void setup() {
        tenantDTO = TestDataUtil.createTestTenantDTO();
        savedTenant = tenantRepository.save(tenantService.convertTenantToEntity(tenantDTO));
    }

    @Test
    void testSubscribeTenant_success() throws Exception {
        mockMvc.perform(post("/api/v1/tenant/subscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tenantDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    void testSubscribeTenant_passwordMismatch() throws Exception {
        tenantDTO.setConfirmPassword("wrongPassword");

        mockMvc.perform(post("/api/v1/tenant/subscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tenantDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Password do not match"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void testListOfTenants() throws Exception {
        mockMvc.perform(get("/api/v1/tenant")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void testGetTenant_found() throws Exception {
        mockMvc.perform(get("/api/v1/tenant/{tenantId}", savedTenant.getId()))
                .andExpect(status().isFound());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void testGetTenant_notFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/tenant/{tenantId}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void testUnsubscribeTenant_success() throws Exception {
        mockMvc.perform(post("/api/v1/tenant/unsubscribe/{tenantId}", savedTenant.getId()))
                .andExpect(status().isOk());
    }
}
