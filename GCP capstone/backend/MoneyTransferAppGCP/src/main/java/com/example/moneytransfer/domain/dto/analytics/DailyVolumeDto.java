package com.example.moneytransfer.domain.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Daily transaction volume returned by Q1 BigQuery analytics query.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyVolumeDto {

    /** Calendar date — "yyyy-MM-dd" format. */
    private String transactionDate;

    /** Day-of-week name, e.g. "Monday". */
    private String dayName;

    /** Total number of transfer attempts on this day. */
    private long totalTransactions;

    /** Number of transfers that completed successfully. */
    private long successfulTransactions;

    /** Number of transfers that failed. */
    private long failedTransactions;

    /** Sum of all successfully transferred amounts. */
    private BigDecimal totalAmountTransferred;

    /** Mean amount across all transfers on this day. */
    private BigDecimal avgTransferAmount;
}
