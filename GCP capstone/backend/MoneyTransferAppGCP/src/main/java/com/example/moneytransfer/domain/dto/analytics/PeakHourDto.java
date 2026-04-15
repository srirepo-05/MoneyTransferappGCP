package com.example.moneytransfer.domain.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Per-hour transaction activity statistics (0–23).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeakHourDto {

    /**
     * Hour of day (0–23, UTC) with the highest transfer activity.
     */
    private int hourOfDay;

    /**
     * Human-readable label, e.g. "14:00 – 14:59".
     */
    private String hourLabel;

    /** Total number of transfers in this hour across all days. */
    private long totalTransactions;

    /** Sum of amounts transferred during this hour. */
    private BigDecimal totalAmount;

    /** Mean amount per transfer during this hour. */
    private BigDecimal avgAmount;

    /**
     * Rank by total transaction count (1 = busiest hour).
     */
    private long rank;
}
