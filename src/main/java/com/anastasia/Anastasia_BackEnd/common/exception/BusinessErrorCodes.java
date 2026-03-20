package com.anastasia.Anastasia_BackEnd.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Getter
@RequiredArgsConstructor
public enum BusinessErrorCodes {

    NO_CODE(0, INTERNAL_SERVER_ERROR, "Unexpected application error", null),

    INCORRECT_PASSWORD(300, BAD_REQUEST, "Password is incorrect", null),
    NEW_PASSWORD_DOES_NOT_MATCH(301, BAD_REQUEST, "New password does not match", null),
    ACCOUNT_LOCKED(302, FORBIDDEN, "User account is locked", "error.code.accountLocked"),
    ACCOUNT_DISABLED(303, FORBIDDEN, "User account is disabled", "error.code.accountDisabled"),
    BAD_CREDENTIALS(304, UNAUTHORIZED, "Login username or password is incorrect", "error.code.badCredentials"),
    DUPLICATE_RESOURCE(305, CONFLICT, "The provided %s is already in use. Please use a different %s.", "error.code.duplicateResourceGeneric"),
    DUPLICATE_REQUEST(306, CONFLICT, "The provided data or name is already in use", "error.code.duplicateRequest"),
    RESOURCE_NOT_FOUND(307, NOT_FOUND, "The requested resource was not found", "error.code.resourceNotFound"),
    INVALID_REQUEST(308, BAD_REQUEST, "The request is invalid or malformed", "error.code.invalidRequest"),
    ACCESS_DENIED(309, FORBIDDEN, "You do not have permission to access this resource", "error.code.accessDenied"),
    AUTHENTICATION_FAILED(310, UNAUTHORIZED, "Authentication failed", "error.code.authenticationFailed"),
    BUSINESS_RULE_VIOLATION(311, BAD_REQUEST, "Business rule violation", "error.code.businessRuleViolation"),
    STATE_CONFLICT(312, CONFLICT, "Resource state conflict", "error.code.stateConflict"),
    DATA_ACCESS_ERROR(313, INTERNAL_SERVER_ERROR, "A data access error occurred", "error.code.dataAccessError"),
    UNSUPPORTED_OPERATION(314, NOT_IMPLEMENTED, "Operation is not supported", "error.code.unsupportedOperation");

    private final int code;
    private final HttpStatus httpStatus;
    private final String description;
    private final String messageKey;

    public String formatDescription(String field) {
        return String.format(description, field, field);
    }
}
