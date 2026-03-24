package com.anastasia.Anastasia_BackEnd.IntegrationTest.controller;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.Api.config.PostgresTestContainer;
import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChurchService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.TenantService;
import com.anastasia.Anastasia_BackEnd.common.utils.JwtUtil;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
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
class ChurchControllerIT extends PostgresTestContainer {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthService authService;
    @Autowired private ChurchService churchService;
    @Autowired private ChurchRepository churchRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private TenantService tenantService;
    @Autowired private JwtUtil jwtUtil;

    @MockitoBean private EmailNotificationService emailNotificationService;
    @Captor private ArgumentCaptor<Map<String, Object>> templatePropertiesCaptor; // Captor for the properties map


    private String jwtToken;
    private ChurchDTO churchDTO;
    private UUID tenantId;
    private TenantEntity tenantEntity;
    private ChurchEntity church;

    @BeforeEach
    void setUp() throws MessagingException {
        MockitoAnnotations.openMocks(this);

        TenantDTO tenantDTO = TestDataUtil.createTestTenantDTO();
        tenantService.subscribeTenant(tenantDTO);

        verify(emailNotificationService).sendEmail(
                eq(tenantDTO.getOwnerEmail()),
                eq("Account Activation for Anastasia"), // Subject
                eq(EmailTemplateName.ACTIVATE_ACCOUNT),
                templatePropertiesCaptor.capture() // Capture the entire properties map
        );

        Map<String, Object> capturedProperties = templatePropertiesCaptor.getValue();
        assertNotNull(capturedProperties);
        // Assert that the map contains the expected values
        assertNotNull(capturedProperties.get("username"));
        assertNotNull(capturedProperties.get("confirmation_url"));
        String token = (String) capturedProperties.get("activation_code"); // Extract the token
        assertNotNull(token);

        assertNotNull(token);
        authService.activateAccount(token);

        AuthenticationResponse auth = authService.authenticate(
                AuthenticationRequest.builder()
                        .email(tenantDTO.getOwnerEmail())
                        .password(tenantDTO.getPassword())
                .build());

        jwtToken = auth.getAccessToken();
        tenantId = UUID.fromString(jwtUtil.extractTenantId(jwtToken));
        tenantEntity = tenantRepository.findById(tenantId).orElse(null);
        TenantContext.setTenantId(tenantId);

//        churchDTO = TestDataUtil.createTestChurchDTO();
//
//        String churchNumber = churchService.createChurch(churchService.convertToEntity(churchDTO));
//        church = churchRepository.findByChurchNumber(churchNumber).orElse(null);
    }

    @Test
    void testRegisterChurch() throws Exception {
        ChurchDTO testChurch = TestDataUtil.createTestChurchDTO_B();
        mockMvc.perform(post("/api/v1/churches/register")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testChurch)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.churchNumber", notNullValue()))
                .andExpect(jsonPath("$.churchName", is(testChurch.getChurchNameLocal())))
                .andExpect(jsonPath("$.dioceseLocal", is(testChurch.getDioceseLocal())))
                .andExpect(jsonPath("$.descriptionLocal", is(testChurch.getDescriptionLocal())));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void testGetChurches() throws Exception {
        churchService.createChurch(churchService.convertToEntity(TestDataUtil.createTestChurchDTO()));
        mockMvc.perform(get("/api/v1/churches")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void testFindChurchById() throws Exception {
        ChurchDTO churchDTO2 = TestDataUtil.createTestChurchDTO_B();
        String churchNumber = churchService.createChurch(churchService.convertToEntity(churchDTO2)).getChurchNumber();
        ChurchEntity church = churchRepository.findByChurchNumber(churchNumber).orElse(null);

        assert church != null;
        mockMvc.perform(get("/api/v1/churches/{id}", church.getChurchId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.churchName", notNullValue()));
    }

    @Test
    void testUpdateChurch() throws Exception {
      ChurchDTO churchDTO1 = TestDataUtil.createTestChurchDTO();
      ChurchEntity churchEntity = churchService.convertToEntity(churchDTO1);

      String churchNum = churchService.createChurch(churchEntity).getChurchNumber();

      ChurchEntity church01 = churchRepository.findByChurchNumber(churchNum).orElse(null);
      churchDTO1.setChurchNameLocal("Updated Church Name");

      assert church01 != null;
      mockMvc.perform(put("/api/v1/churches/{id}", church01.getChurchId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(churchDTO1)))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.churchName", is("Updated Church Name")))
              .andExpect(jsonPath("$.dioceseLocal", is(churchDTO1.getDioceseLocal())))
              .andExpect(jsonPath("$.descriptionLocal", is(churchDTO1.getDescriptionLocal())));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void testDeleteChurch() throws Exception {
        ChurchDTO churchDTO1 = TestDataUtil.createTestChurchDTO();
        String churchNum = churchService.createChurch(churchService.convertToEntity(churchDTO1)).getChurchNumber();
        ChurchEntity church01 = churchRepository.findByChurchNumber(churchNum).orElse(null);

        assert church01 != null;
        mockMvc.perform(delete("/api/v1/churches/{id}", church01.getChurchId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }
}
