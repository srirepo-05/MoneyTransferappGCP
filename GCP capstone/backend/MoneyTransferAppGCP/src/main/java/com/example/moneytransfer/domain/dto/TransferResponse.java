package com.example.moneytransfer.domain.dto;

import com.example.moneytransfer.domain.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Outbound DTO returned after a transfer attempt (success or failure).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {

    private String transactionId;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private TransactionStatus status;
    private String failureReason;
    private String idempotencyKey;
    private LocalDateTime createdOn;
}
