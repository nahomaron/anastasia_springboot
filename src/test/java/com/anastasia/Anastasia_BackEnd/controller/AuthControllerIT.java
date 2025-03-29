package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.AnastasiaBackEndApplication;
import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.config.RateLimiterConfig;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.DTO.auth.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.Token;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.auth.TokenRepository;
import com.anastasia.Anastasia_BackEnd.service.auth.UserServices;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AnastasiaBackEndApplication.class)
//@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class AuthControllerIT {


    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final TokenRepository tokenRepository;
    private final UserServices userServices;

    @Mock
    private RateLimiterConfig rateLimiterConfig;

    public Bucket bucket;


    @BeforeEach
    void setUp() {
        bucket = Bucket.builder()
                .addLimit(io.github.bucket4j.Bandwidth.classic(5, io.github.bucket4j.Refill.greedy(5, java.time.Duration.ofMinutes(1))))
                .build();

        when(rateLimiterConfig.getBucket(Mockito.anyString())).thenReturn(bucket);
    }

    @Autowired
    public AuthControllerIT(MockMvc mockMvc, ObjectMapper objectMapper, TokenRepository tokenRepository, Bucket bucket, UserServices userServices) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.tokenRepository = tokenRepository;
        this.userServices = userServices;
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
        UserEntity user = TestDataUtil.createTestUserEntityA();
        user.setVerified(true);
        userServices.createUser(user);

        AuthenticationRequest testAuth = TestDataUtil.createTestAuthenticationRequest();
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
        userServices.createUser(user);

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

        UserEntity createdUser  = userServices.findUserByEmail(testUserDTOA.getEmail()).orElseThrow();
        Token token = tokenRepository.findByUserUuid(createdUser.getUuid());
        String verificationTokenCode = token.getToken();

        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/v1/auth/activate-account")
                        .param("token", verificationTokenCode)
        ).andExpect(
                status().isOk()
        );


        AuthenticationRequest testAuth = TestDataUtil.createTestAuthenticationRequest();
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
        // should allow requests with in the limit
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/refresh-token")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    public void testRefreshToken_TooManyRequests() throws Exception {

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
