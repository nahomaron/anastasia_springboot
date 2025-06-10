package com.anastasia.Anastasia_BackEnd.exception;

import com.sun.jdi.request.DuplicateRequestException;
import jakarta.mail.MessagingException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.anastasia.Anastasia_BackEnd.exception.BusinessErrorCodes.*;
import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ----------------- Override Spring MVC’s own handlers ----

    // Important to use @Override for overridden methods
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {

        log.error("Malformed JSON request", ex);
        // Ensure the return type matches ResponseEntity<Object>
        return new ResponseEntity<>(buildResponse(BAD_REQUEST, INVALID_REQUEST, "Malformed JSON request").getBody(), BAD_REQUEST);
    }

    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {

        log.error("Request method not supported", ex);
        // Ensure the return type matches ResponseEntity<Object>
        return new ResponseEntity<>(buildResponse(METHOD_NOT_ALLOWED, INVALID_REQUEST,
                String.format("Method %s not supported for this endpoint", ex.getMethod())).getBody(), METHOD_NOT_ALLOWED);
    }


    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {

        log.error("Missing request parameter", ex);
        // Ensure the return type matches ResponseEntity<Object>
        return new ResponseEntity<>(buildResponse(BAD_REQUEST, INVALID_REQUEST,
                String.format("Missing parameter: %s", ex.getParameterName())).getBody(), BAD_REQUEST);
    }

    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {

        log.error("Type mismatch", ex);
        // Ensure the return type matches ResponseEntity<Object>
        return new ResponseEntity<>(buildResponse(BAD_REQUEST, INVALID_REQUEST,
                String.format("Parameter '%s' expects value of type %s",
                        ex.getPropertyName(), ex.getRequiredType().getSimpleName())).getBody(), BAD_REQUEST);
    }

    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {

        log.error("No handler found for request", ex);
        // Ensure the return type matches ResponseEntity<Object>
        return new ResponseEntity<>(buildResponse(NOT_FOUND, RESOURCE_NOT_FOUND,
                String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL())).getBody(), NOT_FOUND);
    }

    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {

        log.error("Media type not supported", ex);
        // Ensure the return type matches ResponseEntity<Object>
        return new ResponseEntity<>(buildResponse(UNSUPPORTED_MEDIA_TYPE, INVALID_REQUEST,
                String.format("Media type %s not supported", ex.getContentType())).getBody(), UNSUPPORTED_MEDIA_TYPE);
    }

    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {

        log.error("Media type not acceptable", ex);
        // Ensure the return type matches ResponseEntity<Object>
        return new ResponseEntity<>(buildResponse(NOT_ACCEPTABLE, INVALID_REQUEST,
                "Requested media type not acceptable").getBody(), NOT_ACCEPTABLE);
    }

    protected ResponseEntity<Object> handleBindException(
            BindException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {

        log.error("Binding failure", ex);
        // Ensure the return type matches ResponseEntity<Object>
        return new ResponseEntity<>(handleValidationErrors(ex.getBindingResult().getAllErrors()).getBody(), BAD_REQUEST);
    }

    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exp, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        Set<String> errors = new HashSet<>();
        exp.getBindingResult().getAllErrors()
                .forEach(error -> {
                    var errorMessage = error.getDefaultMessage();
                    errors.add(errorMessage);
                });

        // Ensure the return type matches ResponseEntity<Object>
        return new ResponseEntity<>(ExceptionResponse.builder()
                .validationErrors(errors)
                .build(), BAD_REQUEST);
    }


    // ============================== Custom / application exceptions ================================== //


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ExceptionResponse> handleArgTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.error("Argument type mismatch", ex);
        return buildResponse(BAD_REQUEST, INVALID_REQUEST,
                String.format("'%s' should be of type %s",
                        ex.getName(), Objects.requireNonNull(ex.getRequiredType()).getSimpleName()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ExceptionResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.error("Constraint violation", ex);
        Set<String> violations = new HashSet<>();
        ex.getConstraintViolations()
                .forEach(v -> violations.add(v.getPropertyPath() + ": " + v.getMessage()));
        return ResponseEntity.status(BAD_REQUEST).body(
                ExceptionResponse.builder()
                        .validationErrors(violations)
                        .build()
        );
    }

    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<ExceptionResponse> handleMessaging(MessagingException ex) {
        log.error("Email error", ex);
        return buildResponse(INTERNAL_SERVER_ERROR, NO_CODE, "Failed to send email: " + ex.getMessage());
    }


    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ExceptionResponse> handleDataAccess(DataAccessException ex) {
        log.error("General data access exception", ex);
        return buildResponse(INTERNAL_SERVER_ERROR, NO_CODE, "Data access error");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionResponse> handleAccessDenied(AccessDeniedException ex) {
        log.error("Access denied", ex);
        return buildResponse(FORBIDDEN, ACCOUNT_DISABLED, "You do not have permission to access this resource");
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ExceptionResponse> handleDataIntegrityViolationException(DataIntegrityViolationException exp) {
        String field = extractDuplicateField(exp); // implement this method
        return ResponseEntity.status(CONFLICT).body(
                ExceptionResponse.builder()
                        .errorCode(DUPLICATE_RESOURCE.getCode())
                        .errorDescription(DUPLICATE_RESOURCE.formatDescription(field))
                        .error(exp.getMostSpecificCause().getMessage())
                        .build()
        );
    }


    //todo -> DuplicateRequestException: Group name already exists
    @ExceptionHandler(DuplicateRequestException.class)
    public ResponseEntity<ExceptionResponse> handleDuplicationRequestException(DuplicateRequestException exception){
        return ResponseEntity.status(CONFLICT).body(
                ExceptionResponse.builder()
                        .errorCode(DUPLICATE_REQUEST.getCode())
                        .errorDescription(DUPLICATE_REQUEST.getDescription())
                        .error(exception.getMessage())
                        .build()
        );
    }

    // This consolidated handler will catch LockedException, DisabledException, and BadCredentialsException
    @ExceptionHandler({LockedException.class, DisabledException.class, BadCredentialsException.class})
    public ResponseEntity<ExceptionResponse> handleAuthExceptions(Exception ex) { // Keep this one
        log.error("Authentication error", ex);
        BusinessErrorCodes code = LockedException.class.isInstance(ex)
                ? ACCOUNT_LOCKED
                : DisabledException.class.isInstance(ex)
                ? ACCOUNT_DISABLED
                : BAD_CREDENTIALS;
        return buildResponse(UNAUTHORIZED, code, code.getDescription());
    }

    // Removed the generic @ExceptionHandler(Exception.class) method to resolve ambiguity
    // with more specific handlers and overridden methods from ResponseEntityExceptionHandler.


    // ===================================== Helper methods ============================================== //

    private ResponseEntity<ExceptionResponse> buildResponse(HttpStatus status,
                                                            BusinessErrorCodes code,
                                                            String message) {
        return ResponseEntity.status(status).body(
                ExceptionResponse.builder()
                        .errorCode(code.getCode())
                        .errorDescription(code.getDescription())
                        .error(message)
                        .build()
        );
    }

    private ResponseEntity<ExceptionResponse> handleValidationErrors(java.util.List<org.springframework.validation.ObjectError> errors) {
        Set<String> messages = new HashSet<>();
        errors.forEach(e -> messages.add(e.getDefaultMessage()));
        return ResponseEntity.status(BAD_REQUEST).body(
                ExceptionResponse.builder()
                        .validationErrors(messages)
                        .build()
        );
    }

    private String extractDuplicateField(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause().getMessage().toLowerCase();
        if (msg.contains("email"))    return "email";
        if (msg.contains("username")) return "username";
        if (msg.contains("phone"))    return "phone number";
        return "resource";
    }
}
