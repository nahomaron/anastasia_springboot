package com.anastasia.Anastasia_BackEnd.UnitTests.common.exception;

import com.anastasia.Anastasia_BackEnd.common.exception.BusinessErrorCodes;
import com.anastasia.Anastasia_BackEnd.common.exception.ExceptionResponse;
import com.anastasia.Anastasia_BackEnd.common.exception.GlobalExceptionHandler;
import com.anastasia.Anastasia_BackEnd.common.exception.customExceptions.AuthenticationProcessException;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private LocalizedMessageService messageService;
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        messageService = mock(LocalizedMessageService.class);
        when(messageService.get(anyString(), anyString(), any(Object[].class))).thenAnswer(invocation -> invocation.getArgument(1));
        when(messageService.resolve(anyString(), any(Object[].class))).thenAnswer(invocation -> invocation.getArgument(0));
        handler = new GlobalExceptionHandler(messageService);
    }

    @Test
    void handleAuthExceptions_returnsStableBadCredentialsMessage() {
        ResponseEntity<ExceptionResponse> response = handler.handleAuthExceptions(
                new BadCredentialsException("ldap server timeout for user foo")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(BusinessErrorCodes.BAD_CREDENTIALS.getCode(), response.getBody().getErrorCode());
        assertEquals("Login username or password is incorrect", response.getBody().getError());
    }

    @Test
    void handleAuthentication_returnsStableAuthenticationFailedMessage() {
        ResponseEntity<ExceptionResponse> response = handler.handleAuthentication(new AuthenticationException("jwt parser failed") {
        });

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(BusinessErrorCodes.AUTHENTICATION_FAILED.getCode(), response.getBody().getErrorCode());
        assertEquals("Authentication failed", response.getBody().getError());
    }

    @Test
    void handleAuthProcess_returnsStableAuthenticationFailedMessage() {
        ResponseEntity<ExceptionResponse> response = handler.handleAuthProcess(
                new AuthenticationProcessException("oauth provider rejected callback")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(BusinessErrorCodes.AUTHENTICATION_FAILED.getCode(), response.getBody().getErrorCode());
        assertEquals("Authentication failed", response.getBody().getError());
    }
}
