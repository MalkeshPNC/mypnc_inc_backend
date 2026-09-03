package com.pnc.masters.customer.api;

import com.pnc.masters.configuration.api.ConfigurationKeyExistsException;
import com.pnc.masters.configuration.api.ConfigurationNotFoundException;
import com.pnc.masters.contact.api.ContactNotFoundException;
import com.pnc.masters.document.api.DocumentNotFoundException;
import com.pnc.masters.document.api.DocumentValidationException;
import com.pnc.masters.ncmaster.api.NcMasterNotFoundException;
import com.pnc.masters.ncmaster.api.NcNumberExistsException;
import com.pnc.masters.salesperson.api.SalesPersonNotFoundException;
import com.pnc.masters.security.api.DuplicateEmailException;
import com.pnc.masters.security.api.InvalidCredentialsException;
import com.pnc.masters.security.api.InvalidResetTokenException;
import com.pnc.masters.security.api.RoleConflictException;
import com.pnc.masters.security.api.RoleNotFoundException;
import com.pnc.masters.security.api.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(CustomerNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ContactNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleContactNotFound(ContactNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(SalesPersonNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSalesPersonNotFound(SalesPersonNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentNotFound(DocumentNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DocumentValidationException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentValidation(DocumentValidationException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(NcMasterNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNcMasterNotFound(NcMasterNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(NcNumberExistsException.class)
    public ResponseEntity<Map<String, Object>> handleNcNumberExists(NcNumberExistsException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ConfigurationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleConfigurationNotFound(ConfigurationNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ConfigurationKeyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleConfigurationKeyExists(ConfigurationKeyExistsException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException exception) {
        return response(HttpStatus.UNAUTHORIZED, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidResetTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidResetToken(InvalidResetTokenException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(DuplicateEmailException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRoleNotFound(RoleNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(RoleConflictException.class)
    public ResponseEntity<Map<String, Object>> handleRoleConflict(RoleConflictException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Request validation failed", errors);
    }

    private ResponseEntity<Map<String, Object>> response(
            HttpStatus status,
            String message,
            Object details
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("details", details);
        return ResponseEntity.status(status).body(body);
    }
}
