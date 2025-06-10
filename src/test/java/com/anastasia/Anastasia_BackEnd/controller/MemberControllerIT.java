package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.member.MemberDTO;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.model.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
import com.anastasia.Anastasia_BackEnd.repository.registration.MemberRepository;
import com.anastasia.Anastasia_BackEnd.service.auth.AuthService;
import com.anastasia.Anastasia_BackEnd.service.email.EmailService;
import com.anastasia.Anastasia_BackEnd.service.email.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.service.registration.ChurchService;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MemberControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ChurchRepository churchRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private AuthService authService;
    @Autowired private ChurchService churchService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;

    @MockBean private EmailService emailService;
    @Captor private ArgumentCaptor<String> tokenCaptor;


    private String jwtToken;
    private ChurchEntity church;
    private MemberDTO memberDTO;


    @BeforeEach
    void setUp() throws MessagingException {
        MockitoAnnotations.openMocks(this);
        TenantEntity tenant = tenantRepository.save(TestDataUtil.createTestTenantEntity());
        TenantContext.setTenantId(tenant.getId());
        String churchNumber = churchService.createChurch(TestDataUtil.createTestChurchEntity(tenant));
        church = churchRepository.findByChurchNumber(churchNumber).orElse(null);

//        UserEntity user = TestDataUtil.createTestUserEntityA();
        UserEntity user = TestDataUtil.createTestUserWithPermissions(
                Set.of(PermissionType.ADD_MEMBERS,
                        PermissionType.EDIT_MEMBERS,
                        PermissionType.VIEW_MEMBERS,
                        PermissionType.APPROVE_MEMBERSHIP,
                        PermissionType.DELETE_MEMBERS,
                        PermissionType.ADVANCED_SEARCH_MEMBERS
                        ),
                tenant,
                roleRepository,
                permissionRepository
        );

        authService.createUser(user);

        // Capture the token passed to emailService
        verify(emailService).sendEmail(
                eq(user.getEmail()),
                eq(user.getFullName()),
                eq(EmailTemplateName.ACTIVATE_ACCOUNT),
                anyString(),
                tokenCaptor.capture(),
                eq("Account Activation")
        );

        String capturedToken = tokenCaptor.getValue();
        assertNotNull(capturedToken);
        authService.activateAccount(capturedToken);
        AuthenticationResponse response = authService.authenticate(
                TestDataUtil.createTestAuthenticationRequest());
        jwtToken = response.getAccessToken();
        memberDTO = TestDataUtil.createTestMemberDTO(church);
    }

    @Test
    void testRegisterMember() throws Exception {
        mockMvc.perform(post("/api/v1/registrar/members/register-member")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.membershipNumber").exists());
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
        MemberEntity saved = memberRepository.save(TestDataUtil.createTestMember(church));

        mockMvc.perform(get("/api/v1/registrar/members/{id}", saved.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isFound())
                .andExpect(jsonPath("$.firstName", is(saved.getFirstName())));
    }

    @Test
    void testUpdateMembershipDetails() throws Exception {
        MemberEntity member = TestDataUtil.createTestMember(church);
        MemberEntity saved = memberRepository.save(member);

        MemberDTO updatedDTO = memberDTO;
        updatedDTO.setFirstName("UpdatedName");

        mockMvc.perform(patch("/api/v1/registrar/members/{id}", saved.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDTO)))
                .andExpect(status().isAccepted());
    }

    @Test
    void testApproveByChurch() throws Exception {
        MemberEntity saved = memberRepository.save(TestDataUtil.createTestMember(church));

        mockMvc.perform(patch("/api/v1/registrar/members/{id}/church-approve", saved.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isAccepted());
    }

    @Test
    @WithMockUser(roles = "PRIEST")
    void testApproveByPriest() throws Exception {
        MemberEntity saved = memberRepository.save(TestDataUtil.createTestMember(church));

        mockMvc.perform(patch("/api/v1/registrar/members/{id}/priest-approve", saved.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isAccepted());
    }

    @Test
    void testDeleteMembership() throws Exception {
        MemberEntity saved = memberRepository.save(TestDataUtil.createTestMember(church));

        mockMvc.perform(delete("/api/v1/registrar/members/{id}", saved.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void testAdvancedSearch() throws Exception {
        memberRepository.save(TestDataUtil.createTestMember(church));

        mockMvc.perform(post("/api/v1/registrar/members/advanced-search")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))  // Add filters if needed
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }
}

