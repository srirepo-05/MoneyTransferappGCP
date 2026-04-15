package com.example.moneytransfer.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Inbound DTO for the fund-transfer endpoint.
 *
 * <p>All fields are mandatory and the amount must be a positive monetary value.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {

    @NotNull(message = "Source account ID is required")
    private Long fromAccountId;

    @NotNull(message = "Destination account ID is required")
    private Long toAccountId;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be at least 0.01")
    @Digits(integer = 15, fraction = 4, message = "Amount format invalid (max 15 integer digits, 4 decimal places)")
    private BigDecimal amount;

    /**
     * Client-supplied UUID that enables idempotent submission.
     * The same key must not be reused for a different transfer.
     */
    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}
