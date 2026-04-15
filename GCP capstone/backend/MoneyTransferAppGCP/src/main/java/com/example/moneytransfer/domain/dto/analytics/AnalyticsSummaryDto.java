package com.example.moneytransfer.domain.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * High-level dashboard summary combining all key analytics KPIs in a single response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsSummaryDto {

    // ── Volume ───────────────────────────────────────────────────────────────

    /** Grand total of all transfer attempts ever recorded. */
    private long totalTransactions;

    /** Count of transfers with status SUCCESS. */
    private long successfulTransactions;

    /** Count of transfers with status FAILED. */
    private long failedTransactions;

    /** Success rate percentage (0–100, 2 decimal places). */
    private BigDecimal successRatePercent;

    // ── Value ────────────────────────────────────────────────────────────────

    /** Cumulative value of all successful transfers. */
    private BigDecimal totalVolumeTransferred;

    /** Average amount per successful transfer. */
    private BigDecimal avgTransferAmount;

    /** Largest single successful transfer ever recorded. */
    private BigDecimal maxTransferAmount;

    /** Smallest single successful transfer ever recorded. */
    private BigDecimal minTransferAmount;

    // ── Activity ─────────────────────────────────────────────────────────────

    /** Number of distinct accounts that have participated in at least one transfer. */
    private long activeAccounts;

    /** Hour of day (0–23) with the highest transfer count. */
    private int peakHour;

    /** Human-readable label for peak hour, e.g. "14:00 – 14:59". */
    private String peakHourLabel;

    // ── Meta ─────────────────────────────────────────────────────────────────

    /** Timestamp when this summary was generated. */
    private LocalDateTime generatedAt;
}
