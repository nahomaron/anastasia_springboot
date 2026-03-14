package com.anastasia.Anastasia_BackEnd.IntegrationTest.controller;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.Api.config.PostgresTestContainer;
import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
        import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Epic("Integration Tests")
@Feature("Internal Layer")
@SpringBootTest
@AutoConfigureMockMvc
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class ChildControllerIT extends PostgresTestContainer {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ChildRepository childRepository;
    @Autowired private ChurchRepository churchRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private AuthService authService;
    @Autowired private ChurchService churchService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;

    @MockitoBean private EmailNotificationService emailNotificationService;
//    @Captor private ArgumentCaptor<String> tokenCaptor;

    @Captor private ArgumentCaptor<Map<String, Object>> templatePropertiesCaptor; // Captor for the properties map


    private String jwtToken;
    private ChurchEntity church;
    private Child_MemberDTO childMemberDTO;

    @BeforeEach
    void setUp() throws MessagingException {
        MockitoAnnotations.openMocks(this);
        TenantEntity tenant = tenantRepository.save(TestDataUtil.createTestTenantEntity());
        TenantContext.setTenantId(tenant.getId());

        String churchNumber = churchService.createChurch(TestDataUtil.createTestChurchEntity(tenant)).getChurchNumber();
        church = churchRepository.findByChurchNumber(churchNumber).orElse(null);

//        UserEntity user = TestDataUtil.createTestUserEntityA();
        UserEntity user = TestDataUtil.createTestUserWithPermissions(
                Set.of(
                        PermissionType.VIEW_CHILDREN,
                        PermissionType.EDIT_CHILDREN,
                        PermissionType.DELETE_CHILDREN,
                        PermissionType.ADD_MEMBERS
                ),
                tenant,
                roleRepository,
                permissionRepository
        );
        authService.createUser(user);

        verify(emailNotificationService).sendEmail(
                eq(user.getEmail()),
                eq("Account Activation for Anastasia"), // Subject
                eq(EmailTemplateName.ACTIVATE_ACCOUNT),
                templatePropertiesCaptor.capture() // Capture the entire properties map
        );

        Map<String, Object> capturedProperties = templatePropertiesCaptor.getValue();
        assertNotNull(capturedProperties);

        assertNotNull(capturedProperties.get("username"));
        assertNotNull(capturedProperties.get("confirmation_url"));
        String token = (String) capturedProperties.get("activation_code"); // Extract the token
        assertNotNull(token);

        assertNotNull(token);
        authService.activateAccount(token);

        AuthenticationResponse authResponse = authService.authenticate(
                TestDataUtil.createTestAuthenticationRequest(user.getEmail())
        );
        jwtToken = authResponse.getAccessToken();

        childMemberDTO = TestDataUtil.createTestChildDTO(church);
    }

    @Test
    void testRegisterChild() throws Exception {
        mockMvc.perform(post("/api/v1/registrar/children/register-child")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childMemberDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.membershipNumber").exists());
    }

    @Test
    void testListOfChildren() throws Exception {
        mockMvc.perform(get("/api/v1/registrar/children")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(0))));
    }

    @Test
    void testGetChild_found() throws Exception {
        Child_MemberEntity saved = childRepository.save(TestDataUtil.createTestChild(church));

        mockMvc.perform(get("/api/v1/registrar/children/{id}", saved.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isFound())
                .andExpect(jsonPath("$.firstName", is(saved.getFirstName())));
    }

    @Test
    void testUpdateChildDetails() throws Exception {
        Child_MemberEntity saved = childRepository.save(TestDataUtil.createTestChild(church));

        Child_MemberDTO updatedDTO = childMemberDTO;
        updatedDTO.setFirstName("UpdatedChild");

        mockMvc.perform(patch("/api/v1/registrar/children/{id}", saved.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDTO)))
                .andExpect(status().isAccepted());
    }

    @Test
    void testDeleteChild() throws Exception {
        Child_MemberEntity saved = childRepository.save(TestDataUtil.createTestChild(church));

        mockMvc.perform(delete("/api/v1/registrar/children/{id}", saved.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "PRIEST")
    void testAdvancedSearch() throws Exception {
        childRepository.save(TestDataUtil.createTestChild(church));

        mockMvc.perform(post("/api/v1/registrar/children/advanced-search")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }
}
