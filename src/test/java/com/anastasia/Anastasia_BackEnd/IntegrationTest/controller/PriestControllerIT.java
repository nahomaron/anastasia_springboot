package com.anastasia.Anastasia_BackEnd.IntegrationTest.controller;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.api.config.PostgresTestContainer;
import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.service.auth.AuthService;
import com.anastasia.Anastasia_BackEnd.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.service.email.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.service.registration.PriestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Epic("Integration Tests")
@Feature("Internal Layer")
@SpringBootTest
@AutoConfigureMockMvc
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class PriestControllerIT extends PostgresTestContainer {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthService authService;
    @Autowired private PriestService priestService;
    @Autowired private PriestRepository priestRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ChurchRepository churchRepository;

    @MockitoBean private EmailNotificationService emailNotificationService;
    @Captor private ArgumentCaptor<Map<String, Object>> templatePropertiesCaptor;

    private String jwtToken;
    private String churchNumber;
    private TenantEntity savedTenant;

    @BeforeEach
    void setUp() throws MessagingException {
        MockitoAnnotations.openMocks(this);

//        TenantDTO tenantDTO = TestDataUtil.createTestTenantDTO();
//        tenantService.subscribeTenant(tenantDTO);

        TenantEntity tenant = TestDataUtil.createTestTenantEntity();
        savedTenant = tenantRepository.save(tenant);
        TenantContext.setTenantId(savedTenant.getId());

        ChurchEntity church = TestDataUtil.createTestChurchEntity(savedTenant);
        churchRepository.save(church);
        churchNumber = church.getChurchNumber();

        PriestDTO priestDTO = TestDataUtil.createTestPriestDTO(church.getChurchNumber());
        priestService.registerPriest(priestDTO);

        verify(emailNotificationService).sendEmail(
                eq(priestDTO.getPersonalEmail()),
                eq("Account Activation for Anastasia"),
                eq(EmailTemplateName.ACTIVATE_ACCOUNT),
                templatePropertiesCaptor.capture()
        );

        Map<String, Object> capturedProperties = templatePropertiesCaptor.getValue();
        assertNotNull(capturedProperties);
        assertNotNull(capturedProperties.get("username"));
        assertEquals(
                priestDTO.getFirstName() + " " + priestDTO.getFatherName() + " " + priestDTO.getGrandFatherName(),
                capturedProperties.get("username")
        );
        assertNotNull(capturedProperties.get("confirmation_url"));
        String token = (String) capturedProperties.get("activation_code"); // Extract the token
        assertNotNull(token);

        assertNotNull(token);
        authService.activateAccount(token);

        AuthenticationResponse auth = authService.authenticate(AuthenticationRequest.builder()
                .email(priestDTO.getPersonalEmail())
                .password(priestDTO.getPassword())
                .build());

        jwtToken = auth.getAccessToken();
    }

    @Test
    void testRegisterPriest() throws Exception {
        PriestDTO priestDTO_B = TestDataUtil.createTestPriestDTO_B(churchNumber);

        mockMvc.perform(post("/api/v1/priests/register")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(priestDTO_B)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void testListOfPriests() throws Exception {
        mockMvc.perform(get("/api/v1/priests")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void testGetPriestById() throws Exception {
        ChurchEntity church = churchRepository.findByChurchNumber(churchNumber).orElse(null);

        PriestDTO priestDTO = TestDataUtil.createTestPriestDTO_B(churchNumber);
        priestService.registerPriest(priestDTO);

        PriestEntity priestEntity = priestRepository.findByPhoneNumber(priestDTO.getPhoneNumber()).orElse(null);

        assert priestEntity != null;
        mockMvc.perform(get("/api/v1/priests/{id}", priestEntity.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isFound());
    }

    @Test
    void testUpdatePriestDetails() throws Exception {
        PriestDTO priestDTO = TestDataUtil.createTestPriestDTO_B(churchNumber);
        priestService.registerPriest(priestDTO);
        PriestEntity saved = priestRepository.findByPhoneNumber(priestDTO.getPhoneNumber()).orElseThrow();

        PriestDTO priestDTO2 = priestService.convertToDTO(saved);
        priestDTO2.setFirstName("Abba Tekle");

        mockMvc.perform(patch("/api/v1/priests/{id}", saved.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(priestDTO2)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.firstName", notNullValue()));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void testDeletePriest() throws Exception {
        PriestDTO priestDTO = TestDataUtil.createTestPriestDTO_B(churchNumber);
        priestService.registerPriest(priestDTO);
        PriestEntity saved = priestRepository.findByPhoneNumber(priestDTO.getPhoneNumber()).orElseThrow();

        mockMvc.perform(post("/api/v1/priests/delete/{id}", saved.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }
}
