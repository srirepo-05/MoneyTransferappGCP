package com.example.moneytransfer.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbound DTO returned by the global exception handler whenever an error occurs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    /** HTTP status code (e.g. 400, 404, 409, 500). */
    private int status;

    /** Short machine-readable error code (e.g. "ACCOUNT_NOT_FOUND"). */
    private String error;

    /** Human-readable description of what went wrong. */
    private String message;

    /** The request path that caused the error. */
    private String path;

    /** Timestamp when the error occurred. */
    private LocalDateTime timestamp;

    /**
     * Optional list of field-level validation errors;
     * populated only for 400 Bad Request responses.
     */
    private List<FieldError> fieldErrors;

    // -----------------------------------------------------------------------
    // Nested type
    // -----------------------------------------------------------------------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldError {
        private String field;
        private String rejectedValue;
        private String message;
    }
}
