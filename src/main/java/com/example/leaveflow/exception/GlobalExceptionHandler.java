package com.example.leaveflow.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Central place for handling exceptions across all controllers.
 *
 * @RestControllerAdvice means: "intercept exceptions thrown from any @RestController
 * and handle them here, returning a JSON error response."
 *
 * Without this, Spring would return a default HTML error page or an unformatted
 * response. This handler ensures all errors return clean, consistent JSON.
 *
 * Stack traces are never sent to the client — only clean error messages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles cases where an Employee or LeaveRequest is not found.
     * Returns: 404 NOT FOUND
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles authentication and authorization failures thrown by AuthUtils.
     * ResponseStatusException is used by AuthUtils.requireAuthenticated() → 401
     * and AuthUtils.requireAdmin() / requireAdminOrSelf() → 403.
     * Returns the HTTP status specified in the exception.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        return buildResponse(
                HttpStatus.valueOf(ex.getStatusCode().value()),
                ex.getReason());
    }

    /**
     * Handles business rule violations (insufficient balance, invalid transitions, etc.).
     * Returns: 400 BAD REQUEST
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles database unique constraint violations (duplicate employee code or email).
     * DataIntegrityViolationException is thrown by Spring when a UNIQUE constraint fails.
     * Returns: 409 CONFLICT
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DataIntegrityViolationException ex) {
        return buildResponse(HttpStatus.CONFLICT, "A record with that value already exists.");
    }

    /**
     * Handles @Valid validation failures on request bodies.
     * MethodArgumentNotValidException is thrown when a DTO field fails validation.
     * We extract the first field error message and return it.
     * Returns: 400 BAD REQUEST
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Catch-all for any unexpected exceptions.
     * Returns: 500 INTERNAL SERVER ERROR
     * We intentionally do NOT expose the internal exception message to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    // Builds a consistent error response body.
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", status.value());
        error.put("message", message);
        return ResponseEntity.status(status).body(error);
    }

}
