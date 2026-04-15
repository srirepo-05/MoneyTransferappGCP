package com.example.moneytransfer.exception;

import com.example.moneytransfer.domain.dto.ErrorResponse;
import com.example.moneytransfer.domain.exception.AccountNotFoundException;
import com.example.moneytransfer.domain.exception.AccountNotActiveException;
import com.example.moneytransfer.domain.exception.DuplicateTransferException;
import com.example.moneytransfer.domain.exception.InsufficientBalanceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -----------------------------------------------------------------------
    // 400 – Validation failures
    // -----------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .rejectedValue(fe.getRejectedValue() == null ? null : fe.getRejectedValue().toString())
                        .message(fe.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_FAILED")
                .message("Request validation failed. Check fieldErrors for details.")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage(), request);
    }

    // -----------------------------------------------------------------------
    // 404 – Not found
    // -----------------------------------------------------------------------

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(
            AccountNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", ex.getMessage(), request);
    }

    // -----------------------------------------------------------------------
    // 409 – Business rule / conflict
    // -----------------------------------------------------------------------

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotActive(
            AccountNotActiveException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "ACCOUNT_NOT_ACTIVE", ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(
            InsufficientBalanceException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateTransferException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTransfer(
            DuplicateTransferException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "DUPLICATE_TRANSFER", ex.getMessage(), request);
    }

    // -----------------------------------------------------------------------
    // 500 – Catch-all
    // -----------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred.", request);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error,
                                                         String message, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
