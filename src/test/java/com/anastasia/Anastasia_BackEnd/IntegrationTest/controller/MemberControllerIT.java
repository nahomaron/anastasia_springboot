package com.anastasia.Anastasia_BackEnd.IntegrationTest.controller;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.Api.config.PostgresTestContainer;
import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.core.auth.support.TestActivationTokenStore;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChurchService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
class MemberControllerIT extends PostgresTestContainer {

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ChurchRepository churchRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private AuthService authService;
    @Autowired private ChurchService churchService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private TestActivationTokenStore activationTokenStore;

    @MockitoBean private EmailNotificationService emailNotificationService;

    private String jwtToken;
    private ChurchEntity church;
    private Adult_MemberDTO adultMemberDTO;


    @BeforeEach
    void setUp() throws MessagingException {
        MockitoAnnotations.openMocks(this);
        TenantEntity tenant = tenantRepository.save(TestDataUtil.createTestTenantEntity());
        TenantContext.setTenantId(tenant.getId());
        String churchNumber = churchService.createChurch(TestDataUtil.createTestChurchEntity(tenant)).getChurchNumber();
        church = churchRepository.findByChurchNumber(churchNumber).orElse(null);

//        UserEntity user = TestDataUtil.createTestUserEntityA();
        UserEntity user = TestDataUtil.createTestUserWithPermissions(
                Set.of(PermissionType.ADD_MEMBERS,
                        PermissionType.EDIT_MEMBERS,
                        PermissionType.VIEW_MEMBERS,
                        PermissionType.APPROVE_MEMBERSHIP,
                        PermissionType.APPROVE_MEMBERSHIP_AS_PRIEST,
                        PermissionType.DELETE_MEMBERS,
                        PermissionType.ADVANCED_SEARCH_MEMBERS
                        ),
                tenant,
                roleRepository,
                permissionRepository
        );

        authService.createUser(user);

        // Capture the token passed to emailService
        verify(emailNotificationService).sendEmail(
                eq(user.getEmail()),
                anyString(),
                anyString(),
                anyString(),
                any()
        );
        String token = activationTokenStore.findByEmail(user.getEmail()).orElseThrow();
        assertNotNull(token);
        authService.activateAccount(token, user.getEmail());

        AuthenticationResponse response = authService.authenticate(
                TestDataUtil.createTestAuthenticationRequest(user.getEmail()));
        jwtToken = response.getAccessToken();
        adultMemberDTO = TestDataUtil.createTestMemberDTO(church);
    }

    @Test
    void testRegisterMember() throws Exception {
                mockMvc.perform(post("/api/v1/registrar/members/register-member")
                        .with(csrf())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adultMemberDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.membershipNumber").exists())
                .andExpect(jsonPath("$.firstName", is(adultMemberDTO.getFirstName())));
    }

    @Test
    void testListOfMembers() throws Exception {
        mockMvc.perform(get("/api/v1/registrar/members")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(0))));
    }

    @Test
    void testGetMember_found() throws Exception {
        Adult_MemberEntity saved = memberRepository.save(TestDataUtil.createTestMember(church));

        mockMvc.perform(get("/api/v1/registrar/members/{id}", saved.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is(saved.getFirstName())));
    }

    @Test
    void testUpdateMembershipDetails() throws Exception {
        Adult_MemberEntity member = TestDataUtil.createTestMember(church);
        Adult_MemberEntity saved = memberRepository.save(member);

        Adult_MemberDTO updatedDTO = adultMemberDTO;
        updatedDTO.setFirstName("UpdatedName");

        mockMvc.perform(patch("/api/v1/registrar/members/{id}", saved.getId())
                        .with(csrf())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("UpdatedName")));
    }

    @Test
    void testApproveByChurch() throws Exception {
        Adult_MemberEntity saved = memberRepository.save(TestDataUtil.createTestMember(church));

        mockMvc.perform(patch("/api/v1/registrar/members/{id}/church-approve", saved.getId())
                        .with(csrf())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedByChurch", is(true)));
    }

    @Test
    void testApproveByPriest() throws Exception {
        Adult_MemberEntity saved = memberRepository.save(TestDataUtil.createTestMember(church));

        mockMvc.perform(patch("/api/v1/registrar/members/{id}/priest-approve", saved.getId())
                        .with(csrf())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedByPriest", is(true)));
    }

    @Test
    void testDeleteMembership() throws Exception {
        Adult_MemberEntity saved = memberRepository.save(TestDataUtil.createTestMember(church));

        mockMvc.perform(delete("/api/v1/registrar/members/{id}", saved.getId())
                        .with(csrf())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void testAdvancedSearch() throws Exception {
        memberRepository.save(TestDataUtil.createTestMember(church));

        mockMvc.perform(post("/api/v1/registrar/members/advanced-search")
                        .with(csrf())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))  // Add filters if needed
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }
}
