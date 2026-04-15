package com.example.moneytransfer.domain.dto;

import com.example.moneytransfer.domain.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Outbound DTO for account-query endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {

    private Long id;
    private String holderName;
    private BigDecimal balance;
    private AccountStatus status;
    private LocalDateTime lastUpdated;
}
