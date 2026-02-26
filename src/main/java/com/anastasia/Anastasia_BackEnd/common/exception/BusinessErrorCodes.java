package com.anastasia.Anastasia_BackEnd.common.exception;

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
    ACCESS_DENIED(309, FORBIDDEN, "You do not have permission to access this resource"),
    AUTHENTICATION_FAILED(310, UNAUTHORIZED, "Authentication failed"),
    BUSINESS_RULE_VIOLATION(311, BAD_REQUEST, "Business rule violation"),
    STATE_CONFLICT(312, CONFLICT, "Resource state conflict"),
    DATA_ACCESS_ERROR(313, INTERNAL_SERVER_ERROR, "A data access error occurred"),
    UNSUPPORTED_OPERATION(314, NOT_IMPLEMENTED, "Operation is not supported"),
    
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
