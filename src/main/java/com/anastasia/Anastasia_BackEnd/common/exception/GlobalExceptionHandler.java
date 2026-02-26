package com.anastasia.Anastasia_BackEnd.common.exception;

import com.anastasia.Anastasia_BackEnd.common.exception.customExceptions.AuthenticationProcessException;
import com.anastasia.Anastasia_BackEnd.common.exception.customExceptions.InvalidCredentialsException;
import com.anastasia.Anastasia_BackEnd.modules.accounting.exception.AccountNotFoundException;
import com.anastasia.Anastasia_BackEnd.modules.accounting.exception.InsufficientFundsException;
import com.anastasia.Anastasia_BackEnd.modules.accounting.exception.InvalidTransactionException;
import com.anastasia.Anastasia_BackEnd.modules.accounting.exception.ReconciliationException;
import com.anastasia.Anastasia_BackEnd.modules.accounting.exception.ResourceNotFoundException;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.anastasia.Anastasia_BackEnd.common.exception.BusinessErrorCodes.ACCESS_DENIED;
import static com.anastasia.Anastasia_BackEnd.common.exception.BusinessErrorCodes.ACCOUNT_DISABLED;
import static com.anastasia.Anastasia_BackEnd.common.exception.BusinessErrorCodes.ACCOUNT_LOCKED;
import static com.anastasia.Anastasia_BackEnd.common.exception.BusinessErrorCodes.AUTHENTICATION_FAILED;
import static com.anastasia.Anastasia_BackEnd.common.exception.BusinessErrorCodes.BAD_CREDENTIALS;
import static com.anastasia.Anastasia_BackEnd.common.exception.BusinessErrorCodes.BUSINESS_RULE_VIOLATION;
import static com.anastasia.Anastasia_BackEnd.common.exception.BusinessErrorCodes.DATA_ACCESS_ERROR;
import static com.anastasia.Anastasia_BackEnd.common.exception.BusinessErrorCodes.DUPLICATE_REQUEST;
import static com.anastasia.Anastasia_BackEnd.common.exception.BusinessErrorCodes.DUPLICATE_RESOURCE;
import static com.anastasia.Anastasia_BackEnd.common.exception.BusinessErrorCodes.INVALID_REQUEST;
import static com.anastasia.Anastasia_BackEnd.common.exception.BusinessErrorCodes.RESOURCE_NOT_FOUND;
import static com.anastasia.Anastasia_BackEnd.common.exception.BusinessErrorCodes.STATE_CONFLICT;
import static com.anastasia.Anastasia_BackEnd.common.exception.BusinessErrorCodes.UNSUPPORTED_OPERATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.error("Malformed JSON request", ex);
        return bodyOnly(buildResponse(BAD_REQUEST, INVALID_REQUEST, "Malformed JSON request"), BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.error("Request method not supported", ex);
        return bodyOnly(
                buildResponse(
                        METHOD_NOT_ALLOWED,
                        INVALID_REQUEST,
                        String.format("Method %s not supported for this endpoint", ex.getMethod())
                ),
                METHOD_NOT_ALLOWED
        );
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.error("Missing request parameter", ex);
        return bodyOnly(
                buildResponse(BAD_REQUEST, INVALID_REQUEST, String.format("Missing parameter: %s", ex.getParameterName())),
                BAD_REQUEST
        );
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.error("Type mismatch", ex);
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        return bodyOnly(
                buildResponse(
                        BAD_REQUEST,
                        INVALID_REQUEST,
                        String.format("Parameter '%s' expects value of type %s", ex.getPropertyName(), expectedType)
                ),
                BAD_REQUEST
        );
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.error("No handler found for request", ex);
        return bodyOnly(
                buildResponse(
                        NOT_FOUND,
                        RESOURCE_NOT_FOUND,
                        String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL())
                ),
                NOT_FOUND
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.error("Media type not supported", ex);
        return bodyOnly(
                buildResponse(
                        UNSUPPORTED_MEDIA_TYPE,
                        INVALID_REQUEST,
                        String.format("Media type %s not supported", ex.getContentType())
                ),
                UNSUPPORTED_MEDIA_TYPE
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.error("Media type not acceptable", ex);
        return bodyOnly(buildResponse(NOT_ACCEPTABLE, INVALID_REQUEST, "Requested media type not acceptable"), NOT_ACCEPTABLE);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ExceptionResponse> handleBindException(BindException ex) {
        log.error("Binding failure", ex);
        return ResponseEntity.status(BAD_REQUEST).body(validationResponse(ex.getFieldErrors(), ex.getGlobalErrors()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.error("Validation error", ex);
        return ResponseEntity.status(BAD_REQUEST)
                .body(validationResponse(ex.getBindingResult().getFieldErrors(), ex.getBindingResult().getGlobalErrors()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ExceptionResponse> handleArgTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.error("Argument type mismatch", ex);
        return buildResponse(
                BAD_REQUEST,
                INVALID_REQUEST,
                String.format("'%s' should be of type %s", ex.getName(), Objects.requireNonNull(ex.getRequiredType()).getSimpleName())
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ExceptionResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.error("Constraint violation", ex);
        Set<String> violations = new HashSet<>();
        ex.getConstraintViolations().forEach(v -> violations.add(v.getPropertyPath() + ": " + v.getMessage()));
        return ResponseEntity.status(BAD_REQUEST).body(
                ExceptionResponse.builder()
                        .errorCode(INVALID_REQUEST.getCode())
                        .errorDescription(INVALID_REQUEST.getDescription())
                        .validationErrors(violations)
                        .build()
        );
    }

    @ExceptionHandler({LockedException.class, DisabledException.class, BadCredentialsException.class, InvalidCredentialsException.class})
    public ResponseEntity<ExceptionResponse> handleAuthExceptions(RuntimeException ex) {
        log.error("Authentication error", ex);
        BusinessErrorCodes code = LockedException.class.isInstance(ex)
                ? ACCOUNT_LOCKED
                : DisabledException.class.isInstance(ex)
                ? ACCOUNT_DISABLED
                : BAD_CREDENTIALS;
        return buildResponse(UNAUTHORIZED, code, ex.getMessage() != null ? ex.getMessage() : code.getDescription());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ExceptionResponse> handleAuthentication(AuthenticationException ex) {
        log.error("Authentication failed", ex);
        return buildResponse(UNAUTHORIZED, AUTHENTICATION_FAILED, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationProcessException.class)
    public ResponseEntity<ExceptionResponse> handleAuthProcess(AuthenticationProcessException ex) {
        log.error("Authentication process error", ex);
        return buildResponse(INTERNAL_SERVER_ERROR, AUTHENTICATION_FAILED, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionResponse> handleAccessDenied(AccessDeniedException ex) {
        log.error("Access denied", ex);
        return buildResponse(FORBIDDEN, ACCESS_DENIED, ACCESS_DENIED.getDescription());
    }

    @ExceptionHandler({EntityNotFoundException.class, ResourceNotFoundException.class, AccountNotFoundException.class})
    public ResponseEntity<ExceptionResponse> handleNotFound(RuntimeException ex) {
        log.error("Resource not found", ex);
        String message = ex.getMessage() != null ? ex.getMessage() : "Requested resource was not found";
        return buildResponse(NOT_FOUND, RESOURCE_NOT_FOUND, message);
    }

    @ExceptionHandler({EntityExistsException.class, DuplicateKeyException.class})
    public ResponseEntity<ExceptionResponse> handleEntityExists(RuntimeException ex) {
        log.error("Duplicate resource", ex);
        return buildResponse(CONFLICT, DUPLICATE_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ExceptionResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation", ex);
        String field = extractDuplicateField(ex);
        return ResponseEntity.status(CONFLICT).body(
                ExceptionResponse.builder()
                        .errorCode(DUPLICATE_RESOURCE.getCode())
                        .errorDescription(DUPLICATE_RESOURCE.formatDescription(field))
                        .error(ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ExceptionResponse> handleDataAccess(DataAccessException ex) {
        log.error("General data access exception", ex);
        return buildResponse(INTERNAL_SERVER_ERROR, DATA_ACCESS_ERROR, DATA_ACCESS_ERROR.getDescription());
    }

    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<ExceptionResponse> handleMessaging(MessagingException ex) {
        log.error("Email error", ex);
        return buildResponse(INTERNAL_SERVER_ERROR, DATA_ACCESS_ERROR, "Failed to send email: " + ex.getMessage());
    }

    @ExceptionHandler({InvalidTransactionException.class, InsufficientFundsException.class, ReconciliationException.class})
    public ResponseEntity<ExceptionResponse> handleBusinessRuleViolation(RuntimeException ex) {
        log.error("Business rule violation", ex);
        return buildResponse(BAD_REQUEST, BUSINESS_RULE_VIOLATION, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Invalid argument", ex);
        return buildResponse(BAD_REQUEST, INVALID_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ExceptionResponse> handleIllegalState(IllegalStateException ex) {
        log.error("Invalid state", ex);
        return buildResponse(CONFLICT, STATE_CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ExceptionResponse> handleNumberFormat(NumberFormatException ex) {
        log.error("Number format exception", ex);
        return buildResponse(BAD_REQUEST, INVALID_REQUEST, "Invalid numeric value in request");
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ExceptionResponse> handleUnsupportedOperation(UnsupportedOperationException ex) {
        log.error("Unsupported operation", ex);
        return buildResponse(NOT_IMPLEMENTED, UNSUPPORTED_OPERATION, ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ExceptionResponse> handleResponseStatus(ResponseStatusException ex) {
        log.error("ResponseStatusException", ex);
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus resolved = status != null ? status : INTERNAL_SERVER_ERROR;

        BusinessErrorCodes code = switch (resolved) {
            case BAD_REQUEST -> INVALID_REQUEST;
            case NOT_FOUND -> RESOURCE_NOT_FOUND;
            case CONFLICT -> DUPLICATE_REQUEST;
            case FORBIDDEN -> ACCESS_DENIED;
            case UNAUTHORIZED -> AUTHENTICATION_FAILED;
            default -> BusinessErrorCodes.NO_CODE;
        };
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return buildResponse(resolved, code, message);
    }

    private ResponseEntity<ExceptionResponse> buildResponse(HttpStatus status, BusinessErrorCodes code, String message) {
        return ResponseEntity.status(status).body(
                ExceptionResponse.builder()
                        .errorCode(code.getCode())
                        .errorDescription(code.getDescription())
                        .error(message)
                        .build()
        );
    }

    private ExceptionResponse validationResponse(java.util.List<FieldError> fieldErrors,
                                                 java.util.List<org.springframework.validation.ObjectError> globalErrors) {
        Map<String, String> errors = new LinkedHashMap<>();
        Set<String> validationErrors = new HashSet<>();

        for (FieldError fieldError : fieldErrors) {
            String message = fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value";
            errors.putIfAbsent(fieldError.getField(), message);
            validationErrors.add(fieldError.getField() + ": " + message);
        }

        for (org.springframework.validation.ObjectError globalError : globalErrors) {
            if (globalError.getDefaultMessage() != null) {
                validationErrors.add(globalError.getDefaultMessage());
            }
        }

        return ExceptionResponse.builder()
                .errorCode(INVALID_REQUEST.getCode())
                .errorDescription(INVALID_REQUEST.getDescription())
                .errors(errors.isEmpty() ? null : errors)
                .validationErrors(validationErrors.isEmpty() ? null : validationErrors)
                .build();
    }

    private String extractDuplicateField(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        if (message == null) {
            return "resource";
        }

        String normalized = message.toLowerCase();
        if (normalized.contains("email")) {
            return "email";
        }
        if (normalized.contains("username")) {
            return "username";
        }
        if (normalized.contains("phone")) {
            return "phone number";
        }
        if (normalized.contains("name")) {
            return "name";
        }
        return "resource";
    }

    private ResponseEntity<Object> bodyOnly(ResponseEntity<ExceptionResponse> response, HttpStatus status) {
        return new ResponseEntity<>(response.getBody(), status);
    }
}
