package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.service.auth.AuthService;
import com.anastasia.Anastasia_BackEnd.service.email.EmailService;
import com.anastasia.Anastasia_BackEnd.service.email.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.service.registration.ChurchService;
import com.anastasia.Anastasia_BackEnd.service.registration.TenantService;
import com.anastasia.Anastasia_BackEnd.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ChurchControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthService authService;
    @Autowired private ChurchService churchService;
    @Autowired private ChurchRepository churchRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private TenantService tenantService;
    @Autowired private JwtUtil jwtUtil;

    @MockBean private EmailService emailService;
    @Captor private ArgumentCaptor<String> tokenCaptor;

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

        verify(emailService).sendEmail(
                eq(tenantDTO.getEmail()),
                eq(tenantDTO.getOwnerName()),
                eq(EmailTemplateName.ACTIVATE_ACCOUNT),
                anyString(),
                tokenCaptor.capture(),
                eq("Account Activation")
        );

        String token = tokenCaptor.getValue();
        assertNotNull(token);
        authService.activateAccount(token);

        AuthenticationResponse auth = authService.authenticate(
                AuthenticationRequest.builder()
                        .email(tenantDTO.getEmail())
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
                .andExpect(content().string(notNullValue()));
    }

    @Test
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
    void testFindChurchById() throws Exception {
        ChurchDTO churchDTO2 = TestDataUtil.createTestChurchDTO_B();
        String churchNumber = churchService.createChurch(churchService.convertToEntity(churchDTO2));
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

      String churchNum = churchService.createChurch(churchEntity);

      ChurchEntity church01 = churchRepository.findByChurchNumber(churchNum).orElse(null);
      churchDTO1.setChurchName("Updated Church Name");

      assert church01 != null;
      mockMvc.perform(put("/api/v1/churches/{id}", church01.getChurchId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(churchDTO1)))
              .andExpect(status().isAccepted());
    }

    @Test
    void testDeleteChurch() throws Exception {
        ChurchDTO churchDTO1 = TestDataUtil.createTestChurchDTO();
        String churchNum = churchService.createChurch(churchService.convertToEntity(churchDTO1));
        ChurchEntity church01 = churchRepository.findByChurchNumber(churchNum).orElse(null);

        assert church01 != null;
        mockMvc.perform(delete("/api/v1/churches/{id}", church01.getChurchId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }
}
