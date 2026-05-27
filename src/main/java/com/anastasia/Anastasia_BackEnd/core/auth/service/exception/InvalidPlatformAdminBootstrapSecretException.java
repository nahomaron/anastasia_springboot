package com.anastasia.Anastasia_BackEnd.core.auth.service.exception;

import org.springframework.security.access.AccessDeniedException;

public class InvalidPlatformAdminBootstrapSecretException extends AccessDeniedException {

    public InvalidPlatformAdminBootstrapSecretException(String message) {
        super(message);
    }
}
