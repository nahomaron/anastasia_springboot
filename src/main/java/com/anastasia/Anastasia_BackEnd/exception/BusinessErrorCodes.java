package com.anastasia.Anastasia_BackEnd.exception;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;


@Getter
@RequiredArgsConstructor
public enum BusinessErrorCodes {

    NO_CODE(0, NOT_IMPLEMENTED, "No Code"),
    INCORRECT_PASSWORD(300, BAD_REQUEST, "Password is Incorrect"),
    NEW_PASSWORD_DOES_NOT_MATCH(301, BAD_REQUEST, "New password doesn't match"),
    ACCOUNT_DISABLED(303, FORBIDDEN, "User account is disabled"),
    ACCOUNT_LOCKED(302, FORBIDDEN, "User account is locked"),
    BAD_CREDENTIALS(304, FORBIDDEN, "Login username or password is incorrect"),
    DUPLICATE_RESOURCE(305, CONFLICT, "The provided %s is already in use. Please use a different %s."),
    DUPLICATE_REQUEST(306, CONFLICT, "The provided data or name already in use"),
    RESOURCE_NOT_FOUND(307, NOT_FOUND, "The requested resource with %s '%s' was not found"),
    INVALID_REQUEST(308, BAD_REQUEST, "The request is invalid or malformed"),
    
    ;

    @Getter
    private final int code;

    @Getter
    private final HttpStatus httpStatus;

    @Getter
    private final String description;

    public String formatDescription(String field) {
        return String.format(description, field, field);
    }

}
