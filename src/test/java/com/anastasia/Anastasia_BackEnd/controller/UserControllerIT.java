//package com.anastasia.Anastasia_BackEnd.controller;
//
//import com.anastasia.Anastasia_BackEnd.TestDataUtil;
//import com.anastasia.Anastasia_BackEnd.config.TenantContext;
//import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
//import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
//import com.anastasia.Anastasia_BackEnd.model.auth.ChangePasswordRequest;
//import com.anastasia.Anastasia_BackEnd.model.role.AssignRolesRequest;
//import com.anastasia.Anastasia_BackEnd.model.tenant.TenantDTO;
//import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;
//import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
//import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
//import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
//import com.anastasia.Anastasia_BackEnd.service.auth.AuthService;
//import com.anastasia.Anastasia_BackEnd.service.auth.user.UserService;
//import com.anastasia.Anastasia_BackEnd.service.email.EmailService;
//import com.anastasia.Anastasia_BackEnd.service.email.EmailTemplateName;
//import com.anastasia.Anastasia_BackEnd.util.JwtUtil;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.mail.MessagingException;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Captor;
//import org.mockito.MockitoAnnotations;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.List;
//import java.util.Set;
//import java.util.UUID;
//
//import static org.hamcrest.Matchers.*;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.verify;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//class UserControllerIT {
//
//    @Autowired private MockMvc mockMvc;
//    @Autowired private ObjectMapper objectMapper;
//    @Autowired private AuthService authService;
//    @Autowired private UserRepository userRepository;
//    @Autowired private UserService userService;
//    @Autowired private TenantRepository tenantRepository;
//    @Autowired private JwtUtil jwtUtil;
//
//    @MockBean private EmailService emailService;
//    @Captor private ArgumentCaptor<String> tokenCaptor;
//
//    private String jwtToken;
//    private UserEntity savedUser;
//
//    @BeforeEach
//    void setUp() throws MessagingException {
//        MockitoAnnotations.openMocks(this);
//
//        TenantDTO tenantDTO = TestDataUtil.createTestTenantDTO();
//        UserDTO userDTO = TestDataUtil.createTestUserDTO();
//        authService.createUser(userService.convertToEntity(userDTO));
//
//        verify(emailService).sendEmail(
//                eq(tenantDTO.getEmail()),
//                eq(tenantDTO.getOwnerName()),
//                eq(EmailTemplateName.ACTIVATE_ACCOUNT),
//                anyString(),
//                tokenCaptor.capture(),
//                eq("Account Activation")
//        );
//
//        String token = tokenCaptor.getValue();
//        assertNotNull(token);
//        authService.activateAccount(token);
//
//        AuthenticationResponse auth = authService.authenticate(
//                AuthenticationRequest.builder()
//                        .email(tenantDTO.getEmail())
//                        .password(tenantDTO.getPassword())
//                        .build());
//
//        jwtToken = auth.getAccessToken();
//        UUID tenantId = UUID.fromString(jwtUtil.extractTenantId(jwtToken));
//        TenantContext.setTenantId(tenantId);
//
//        savedUser = userRepository.findByEmail(tenantDTO.getEmail()).orElseThrow();
//    }
//
//    @Test
//    void testGetDashboard() throws Exception {
//        mockMvc.perform(get("/api/v1/users/dashboard")
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void testListOfUsers() throws Exception {
//        mockMvc.perform(get("/api/v1/users/")
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$", hasItem(savedUser.getUuid().toString())));
//    }
//
//    @Test
//    void testGetUserById() throws Exception {
//        mockMvc.perform(get("/api/v1/users/{userid}", savedUser.getUuid())
//                        .header("Authorization", "Bearer " + jwtToken))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.email", is(savedUser.getEmail())));
//    }
//
//    @Test
//    void testChangePassword() throws Exception {
//        ChangePasswordRequest request = new ChangePasswordRequest(
//                "password", "newPassword", "newPassword");
//
//        mockMvc.perform(patch("/api/v1/users/change-password")
//                        .header("Authorization", "Bearer " + jwtToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isAccepted());
//    }
//
//    @Test
//    void testUpdateUserDetails() throws Exception {
//        UserDTO dto = TestDataUtil.createTestUserDTO();
//        dto.setFullName("Updated Name");
//
//        mockMvc.perform(patch("/api/v1/users/update-user-details")
//                        .header("Authorization", "Bearer " + jwtToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(dto)))
//                .andExpect(status().isAccepted())
//                .andExpect(jsonPath("$.fullName", is("Updated Name")));
//    }
//
//    @Test
//    void testAssignRolesToUser() throws Exception {
//        AssignRolesRequest request = new AssignRolesRequest(Set.of());
//
//        mockMvc.perform(put("/api/v1/users/{userId}/assign-roles", savedUser.getUuid())
//                        .header("Authorization", "Bearer " + jwtToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk());
//    }
//}
//
