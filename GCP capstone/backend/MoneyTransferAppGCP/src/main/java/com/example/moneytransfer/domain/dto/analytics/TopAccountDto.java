package com.example.moneytransfer.domain.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Account activity ranking entry (most active accounts by transfer count).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopAccountDto {

    /** Account ID as stored in MySQL / BigQuery. */
    private long accountId;

    /** Full name of the account holder. */
    private String holderName;

    /** Current account status: ACTIVE / LOCKED / CLOSED. */
    private String status;

    /** Number of transfers initiated by this account. */
    private long transfersSent;

    /** Number of transfers received by this account. */
    private long transfersReceived;

    /** Combined sent + received (used for ranking). */
    private long totalActivity;

    /** Total value sent by this account (successful transfers only). */
    private BigDecimal totalSent;

    /** Total value received by this account (successful transfers only). */
    private BigDecimal totalReceived;

    /**
     * Net flow = totalReceived - totalSent.
     * Positive → net receiver; negative → net sender.
     */
    private BigDecimal netFlow;

    /** Dense rank (1 = most active). */
    private long activityRank;
}
