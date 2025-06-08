package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.service.auth.AuthService;
import com.anastasia.Anastasia_BackEnd.service.email.EmailService;
import com.anastasia.Anastasia_BackEnd.service.email.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.service.registration.PriestService;
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

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PriestControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthService authService;
    @Autowired private PriestService priestService;
    @Autowired private PriestRepository priestRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ChurchRepository churchRepository;

    @MockBean private EmailService emailService;
    @Captor private ArgumentCaptor<String> tokenCaptor;

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

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService).sendEmail(
                eq(priestDTO.getPersonalEmail()),
                nameCaptor.capture(),
                eq(EmailTemplateName.ACTIVATE_ACCOUNT),
                anyString(),
                tokenCaptor.capture(),
                eq("Account Activation")
        );

        assertEquals(priestDTO.getFirstName() + " " + priestDTO.getFatherName() + " " + priestDTO.getGrandFatherName(), nameCaptor.getValue());


        String token = tokenCaptor.getValue();
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
    void testListOfPriests() throws Exception {
        mockMvc.perform(get("/api/v1/priests")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
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
    void testDeletePriest() throws Exception {
        PriestDTO priestDTO = TestDataUtil.createTestPriestDTO_B(churchNumber);
        priestService.registerPriest(priestDTO);
        PriestEntity saved = priestRepository.findByPhoneNumber(priestDTO.getPhoneNumber()).orElseThrow();

        mockMvc.perform(post("/api/v1/priests/delete/{id}", saved.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }
}
