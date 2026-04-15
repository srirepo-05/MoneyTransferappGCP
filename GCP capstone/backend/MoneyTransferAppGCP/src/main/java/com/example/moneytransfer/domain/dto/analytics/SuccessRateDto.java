package com.example.moneytransfer.domain.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Overall and per-status success-rate statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuccessRateDto {

    /** Total number of transfer attempts across all time. */
    private long totalTransactions;

    /** Number of successfully completed transfers. */
    private long successfulTransactions;

    /** Number of failed transfers. */
    private long failedTransactions;

    /**
     * Success rate as a percentage, e.g. {@code 95.50} for 95.5 %.
     * Rounded to two decimal places.
     */
    private BigDecimal successRatePercent;

    /** Sum of all amounts that were successfully transferred. */
    private BigDecimal totalVolumeTransferred;

    /** Mean amount across successful transfers only. */
    private BigDecimal avgSuccessfulAmount;
}
