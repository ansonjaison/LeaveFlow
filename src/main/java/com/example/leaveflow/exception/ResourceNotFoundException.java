package com.example.leaveflow.exception;

/**
 * Thrown when a requested resource (Employee or LeaveRequest) does not exist.
 *
 * Extends RuntimeException so we don't need to declare it in method signatures.
 * The GlobalExceptionHandler catches this and returns a 404 NOT FOUND response.
 *
 * Example usage:
 *   throw new ResourceNotFoundException("Employee not found with id: " + id);
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

}
