package com.example.leaveflow.exception;

/**
 * Thrown when a business rule is violated.
 *
 * Examples:
 *   - Insufficient leave balance
 *   - Inactive employee trying to apply for leave
 *   - Trying to approve an already-approved request
 *   - Start date is after end date
 *   - Zero working days in the selected date range
 *
 * The GlobalExceptionHandler catches this and returns a 400 BAD REQUEST response.
 *
 * Keeping ResourceNotFoundException and BusinessException separate makes it
 * easy to map them to different HTTP status codes.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

}
