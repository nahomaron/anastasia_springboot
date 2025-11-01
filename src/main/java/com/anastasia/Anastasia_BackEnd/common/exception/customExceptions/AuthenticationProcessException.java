package com.anastasia.Anastasia_BackEnd.common.exception.customExceptions;

public class AuthenticationProcessException extends RuntimeException {
    public AuthenticationProcessException(String message) {
        super(message);
    }
}
