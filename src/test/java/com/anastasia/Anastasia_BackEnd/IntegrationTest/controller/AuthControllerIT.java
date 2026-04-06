package com.anastasia.Anastasia_BackEnd.IntegrationTest.controller;

import com.anastasia.Anastasia_BackEnd.AnastasiaBackEndApplication;
import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.Api.config.PostgresTestContainer;
import com.anastasia.Anastasia_BackEnd.Api.utils.TestDataSeeder;
import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Epic("Integration Tests")
@Feature("Internal Layer")
@SpringBootTest(classes = AnastasiaBackEndApplication.class)
//@ExtendWith(SpringExtension.class)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@Transactional
public class AuthControllerIT extends PostgresTestContainer {


    @Autowired private final MockMvc mockMvc;
    @Autowired private final ObjectMapper objectMapper;
    @Autowired private final TokenRepository tokenRepository;
    @Autowired private final AuthService authService;
    @Autowired private TestDataSeeder testDataSeeder;

    @MockitoBean
    private RateLimiterService rateLimiterService;


    @BeforeEach
    void setUp() {
        Mockito.reset(rateLimiterService);
        testDataSeeder.createAdminUser();
        testDataSeeder.createMember("Nahom");
        when(rateLimiterService.tryConsume(Mockito.anyString(), Mockito.anyLong(), Mockito.any(Duration.class))).thenReturn(true);
    }

    @Autowired
    public AuthControllerIT(MockMvc mockMvc, ObjectMapper objectMapper, TokenRepository tokenRepository, AuthService authService) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.tokenRepository = tokenRepository;
        this.authService = authService;
    }

    @Test
    public void testThatSignUpSuccessfullyReturns201Created() throws Exception {
        UserDTO testUserDTOA = TestDataUtil.createTestUserDTO();
        String userJson = objectMapper.writeValueAsString(testUserDTOA);
        mockMvc.perform(
                post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson)
        ).andExpect(
                status().isCreated()
        );
    }

    @Test
    public void testThatLoginReturnsHttpStatus200OnSuccess() throws Exception {
        AuthenticationRequest testAuth = new AuthenticationRequest(
                TestDataSeeder.ADMIN_EMAIL,
                TestDataSeeder.ADMIN_PASSWORD
        );
        String testAuthJson = objectMapper.writeValueAsString(testAuth);

        mockMvc.perform(
                    post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(testAuthJson)
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    public void testThatActivateAccountReturnsHttpStatus200Ok() throws Exception {

        UserEntity user = TestDataUtil.createTestUserEntityA();
        authService.createUser(user);

        UserDTO testUserDTOA = TestDataUtil.createTestUserDTO();

        Token token = tokenRepository.findByUserUuid(user.getUuid());
        String verificationTokenCode = token.getToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/v1/auth/activate-account")
                        .param("token", verificationTokenCode)
        ).andExpect(
                status().isOk()
        );
    }

    @Test
    public void testFullAuthFlow() throws Exception {

        UserDTO testUserDTOA = TestDataUtil.createTestUserDTO();
        String userJson = objectMapper.writeValueAsString(testUserDTOA);

        mockMvc.perform(
                post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson)
        ).andExpect(
                status().isCreated()
        );

        UserEntity createdUser  = authService.findUserByEmail(testUserDTOA.getEmail()).orElseThrow();
        Token token = tokenRepository.findByUserUuid(createdUser.getUuid());
        String verificationTokenCode = token.getToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/v1/auth/activate-account")
                        .param("token", verificationTokenCode)
        ).andExpect(
                status().isOk()
        );


        AuthenticationRequest testAuth = TestDataUtil.createTestAuthenticationRequest(testUserDTOA.getEmail());
        String testAuthJson = objectMapper.writeValueAsString(testAuth);

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(testAuthJson)
                )
                .andExpect(
                        status().isOk()
                );


    }

    @Test
    public void testThatRefreshTokenReturnsHttpStatus200Ok() throws Exception {
        when(rateLimiterService.tryConsume(eq("127.0.0.1"), eq(5L), eq(Duration.ofMinutes(1))))
                .thenReturn(true);
        // should allow requests with in the limit
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/refresh-token")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testRefreshToken_TooManyRequests() throws Exception {

        when(rateLimiterService.tryConsume(eq("127.0.0.1"), eq(5L), eq(Duration.ofMinutes(1))))
                .thenReturn(true, true, true, true, true, false);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/refresh-token")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // 6th request should be blocked
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests()) // 429
                .andExpect(content().string("Too many requests, try again later"));
    }

}
