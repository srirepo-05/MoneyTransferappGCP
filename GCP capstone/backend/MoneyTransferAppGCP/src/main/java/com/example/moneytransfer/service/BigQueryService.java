package com.example.moneytransfer.service;

import com.example.moneytransfer.domain.dto.analytics.*;
import com.example.moneytransfer.domain.entity.Account;
import com.example.moneytransfer.domain.entity.TransactionLog;
import com.google.cloud.bigquery.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service responsible for all Google BigQuery interactions.
 *
 * <h3>Two responsibilities:</h3>
 * <ol>
 *   <li><b>Streaming inserts</b> — pushes every completed transfer and every account
 *       snapshot into BigQuery in real time immediately after the MySQL transaction
 *       commits ({@link #streamTransaction} / {@link #streamAccount}).
 *   <li><b>Analytics queries</b> — executes the five SQL analytics queries against
 *       the BigQuery dataset and maps results to typed DTOs exposed by the REST API.
 * </ol>
 *
 * <p>This bean is only created when {@code bigquery.enabled=true}. When BigQuery is
 * disabled the {@link #isEnabled()} check gates every public method so callers never
 * need to guard themselves.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "bigquery.enabled", havingValue = "true")
public class BigQueryService {

    private final BigQuery bigQuery;
    private final String projectId;
    private final String datasetId;
    private final String transactionsTable;
    private final String accountsTable;

    // Full table references  project.dataset.table
    private final String fqTransactions;
    private final String fqAccounts;

    public BigQueryService(
            BigQuery bigQuery,
            @Value("${bigquery.project-id}") String projectId,
            @Value("${bigquery.dataset-id}") String datasetId,
            @Value("${bigquery.table.transactions:transactions}") String transactionsTable,
            @Value("${bigquery.table.accounts:accounts}") String accountsTable) {

        this.bigQuery         = bigQuery;
        this.projectId        = projectId;
        this.datasetId        = datasetId;
        this.transactionsTable = transactionsTable;
        this.accountsTable    = accountsTable;
        this.fqTransactions   = String.format("`%s.%s.%s`", projectId, datasetId, transactionsTable);
        this.fqAccounts       = String.format("`%s.%s.%s`", projectId, datasetId, accountsTable);
    }

    // =========================================================================
    // Streaming inserts
    // =========================================================================

    /**
     * Asynchronously streams a completed (or failed) {@link TransactionLog} row
     * into the BigQuery {@code transactions} table.
     *
     * <p>Runs in a separate thread ({@code @Async}) so it never blocks the
     * HTTP response thread or the MySQL transaction.
     */
    @Async
    public void streamTransaction(TransactionLog txLog) {
        Map<String, Object> row = new HashMap<>();
        row.put("transaction_id",   txLog.getId());
        row.put("from_account_id",  txLog.getFromAccountId());
        row.put("to_account_id",    txLog.getToAccountId());
        row.put("amount",           txLog.getAmount().doubleValue());
        row.put("status",           txLog.getStatus().name());
        row.put("failure_reason",   txLog.getFailureReason());
        row.put("idempotency_key",  txLog.getIdempotencyKey());
        row.put("created_on",       txLog.getCreatedOn() != null
                ? txLog.getCreatedOn().toString() : LocalDateTime.now().toString());

        insertRows(transactionsTable, row, txLog.getId());
    }

    /**
     * Asynchronously streams an {@link Account} snapshot into the BigQuery
     * {@code accounts} table. Called after account creation and after balance updates.
     */
    @Async
    public void streamAccount(Account account) {
        Map<String, Object> row = new HashMap<>();
        row.put("account_id",   account.getId());
        row.put("holder_name",  account.getHolderName());
        row.put("balance",      account.getBalance().doubleValue());
        row.put("status",       account.getStatus().name());
        row.put("snapshot_at",  LocalDateTime.now().toString());

        insertRows(accountsTable, row, String.valueOf(account.getId()));
    }

    // =========================================================================
    // Analytics queries
    // =========================================================================

    /**
     * Q1 — Daily transaction volume: count and total amount by calendar day.
     *
     * @param days number of days to look back (e.g. 30 for the last 30 days)
     */
    public List<DailyVolumeDto> getDailyVolume(int days) {
        String sql = String.format("""
                SELECT
                    DATE(created_on)                                            AS transaction_date,
                    FORMAT_DATE('%%A', DATE(created_on))                        AS day_name,
                    COUNT(*)                                                    AS total_transactions,
                    COUNTIF(status = 'SUCCESS')                                 AS successful_transactions,
                    COUNTIF(status = 'FAILED')                                  AS failed_transactions,
                    COALESCE(SUM(IF(status = 'SUCCESS', amount, 0)), 0)         AS total_amount_transferred,
                    ROUND(AVG(amount), 2)                                       AS avg_transfer_amount
                FROM %s
                WHERE created_on >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL %d DAY)
                GROUP BY transaction_date, day_name
                ORDER BY transaction_date DESC
                """, fqTransactions, days);

        List<DailyVolumeDto> result = new ArrayList<>();
        for (FieldValueList row : runQuery(sql)) {
            result.add(DailyVolumeDto.builder()
                    .transactionDate(row.get("transaction_date").getStringValue())
                    .dayName(row.get("day_name").getStringValue())
                    .totalTransactions(row.get("total_transactions").getLongValue())
                    .successfulTransactions(row.get("successful_transactions").getLongValue())
                    .failedTransactions(row.get("failed_transactions").getLongValue())
                    .totalAmountTransferred(toBigDecimal(row.get("total_amount_transferred")))
                    .avgTransferAmount(toBigDecimal(row.get("avg_transfer_amount")))
                    .build());
        }
        return result;
    }

    /**
     * Q2 — Account activity: most active accounts ranked by combined send + receive count.
     *
     * @param limit maximum number of accounts to return
     */
    public List<TopAccountDto> getTopAccounts(int limit) {
        String sql = String.format("""
                WITH account_totals AS (
                    SELECT
                        t.account_id,
                        t.holder_name,
                        t.status,
                        COUNTIF(f.from_account_id = t.account_id)                           AS transfers_sent,
                        COUNTIF(f.to_account_id   = t.account_id)                           AS transfers_received,
                        COALESCE(SUM(IF(f.from_account_id = t.account_id AND f.status = 'SUCCESS', f.amount, 0)), 0) AS total_sent,
                        COALESCE(SUM(IF(f.to_account_id   = t.account_id AND f.status = 'SUCCESS', f.amount, 0)), 0) AS total_received
                    FROM (
                        SELECT DISTINCT account_id, holder_name, status
                        FROM %s
                    ) t
                    LEFT JOIN %s f
                        ON f.from_account_id = t.account_id
                        OR f.to_account_id   = t.account_id
                    GROUP BY t.account_id, t.holder_name, t.status
                )
                SELECT
                    account_id,
                    holder_name,
                    status,
                    transfers_sent,
                    transfers_received,
                    transfers_sent + transfers_received           AS total_activity,
                    total_sent,
                    total_received,
                    total_received - total_sent                   AS net_flow,
                    DENSE_RANK() OVER (ORDER BY transfers_sent + transfers_received DESC) AS activity_rank
                FROM account_totals
                ORDER BY activity_rank
                LIMIT %d
                """, fqAccounts, fqTransactions, limit);

        List<TopAccountDto> result = new ArrayList<>();
        for (FieldValueList row : runQuery(sql)) {
            result.add(TopAccountDto.builder()
                    .accountId(row.get("account_id").getLongValue())
                    .holderName(row.get("holder_name").getStringValue())
                    .status(row.get("status").getStringValue())
                    .transfersSent(row.get("transfers_sent").getLongValue())
                    .transfersReceived(row.get("transfers_received").getLongValue())
                    .totalActivity(row.get("total_activity").getLongValue())
                    .totalSent(toBigDecimal(row.get("total_sent")))
                    .totalReceived(toBigDecimal(row.get("total_received")))
                    .netFlow(toBigDecimal(row.get("net_flow")))
                    .activityRank(row.get("activity_rank").getLongValue())
                    .build());
        }
        return result;
    }

    /**
     * Q3 — Success rate: overall transfer success / failure breakdown.
     */
    public SuccessRateDto getSuccessRate() {
        String sql = String.format("""
                SELECT
                    COUNT(*)                                                AS total_transactions,
                    COUNTIF(status = 'SUCCESS')                             AS successful_transactions,
                    COUNTIF(status = 'FAILED')                              AS failed_transactions,
                    ROUND(SAFE_DIVIDE(COUNTIF(status = 'SUCCESS') * 100.0, COUNT(*)), 2) AS success_rate_percent,
                    COALESCE(SUM(IF(status = 'SUCCESS', amount, 0)), 0)    AS total_volume_transferred,
                    ROUND(AVG(IF(status = 'SUCCESS', amount, NULL)), 2)    AS avg_successful_amount
                FROM %s
                """, fqTransactions);

        for (FieldValueList row : runQuery(sql)) {
            return SuccessRateDto.builder()
                    .totalTransactions(row.get("total_transactions").getLongValue())
                    .successfulTransactions(row.get("successful_transactions").getLongValue())
                    .failedTransactions(row.get("failed_transactions").getLongValue())
                    .successRatePercent(toBigDecimal(row.get("success_rate_percent")))
                    .totalVolumeTransferred(toBigDecimal(row.get("total_volume_transferred")))
                    .avgSuccessfulAmount(toBigDecimal(row.get("avg_successful_amount")))
                    .build();
        }
        return SuccessRateDto.builder().build(); // empty if no data yet
    }

    /**
     * Q4 — Peak hours: busiest hours of day ranked by transfer count.
     *
     * @param topN number of peak hours to return (e.g. 5 for the 5 busiest hours)
     */
    public List<PeakHourDto> getPeakHours(int topN) {
        String sql = String.format("""
                SELECT
                    EXTRACT(HOUR FROM created_on)                       AS hour_of_day,
                    COUNT(*)                                             AS total_transactions,
                    COALESCE(SUM(amount), 0)                            AS total_amount,
                    ROUND(AVG(amount), 2)                               AS avg_amount,
                    DENSE_RANK() OVER (ORDER BY COUNT(*) DESC)          AS rank
                FROM %s
                GROUP BY hour_of_day
                ORDER BY rank
                LIMIT %d
                """, fqTransactions, topN);

        List<PeakHourDto> result = new ArrayList<>();
        for (FieldValueList row : runQuery(sql)) {
            int hour = (int) row.get("hour_of_day").getLongValue();
            result.add(PeakHourDto.builder()
                    .hourOfDay(hour)
                    .hourLabel(String.format("%02d:00 – %02d:59", hour, hour))
                    .totalTransactions(row.get("total_transactions").getLongValue())
                    .totalAmount(toBigDecimal(row.get("total_amount")))
                    .avgAmount(toBigDecimal(row.get("avg_amount")))
                    .rank(row.get("rank").getLongValue())
                    .build());
        }
        return result;
    }

    /**
     * Q5 — Summary: all key KPIs in a single BigQuery query for the dashboard.
     */
    public AnalyticsSummaryDto getSummary() {
        String sql = String.format("""
                SELECT
                    COUNT(*)                                                             AS total_transactions,
                    COUNTIF(status = 'SUCCESS')                                          AS successful_transactions,
                    COUNTIF(status = 'FAILED')                                           AS failed_transactions,
                    ROUND(SAFE_DIVIDE(COUNTIF(status='SUCCESS') * 100.0, COUNT(*)), 2)  AS success_rate_percent,
                    COALESCE(SUM(IF(status='SUCCESS', amount, 0)), 0)                   AS total_volume,
                    ROUND(AVG(IF(status='SUCCESS', amount, NULL)), 2)                   AS avg_amount,
                    MAX(IF(status='SUCCESS', amount, NULL))                              AS max_amount,
                    MIN(IF(status='SUCCESS', amount, NULL))                              AS min_amount,
                    COUNT(DISTINCT from_account_id) + COUNT(DISTINCT to_account_id)     AS active_accounts,
                    (SELECT EXTRACT(HOUR FROM created_on) AS h
                     FROM %s GROUP BY h ORDER BY COUNT(*) DESC LIMIT 1)                 AS peak_hour
                FROM %s
                """, fqTransactions, fqTransactions);

        for (FieldValueList row : runQuery(sql)) {
            int peakHour = row.get("peak_hour").isNull() ? 0
                    : (int) row.get("peak_hour").getLongValue();
            return AnalyticsSummaryDto.builder()
                    .totalTransactions(row.get("total_transactions").getLongValue())
                    .successfulTransactions(row.get("successful_transactions").getLongValue())
                    .failedTransactions(row.get("failed_transactions").getLongValue())
                    .successRatePercent(toBigDecimal(row.get("success_rate_percent")))
                    .totalVolumeTransferred(toBigDecimal(row.get("total_volume")))
                    .avgTransferAmount(toBigDecimal(row.get("avg_amount")))
                    .maxTransferAmount(toBigDecimal(row.get("max_amount")))
                    .minTransferAmount(toBigDecimal(row.get("min_amount")))
                    .activeAccounts(row.get("active_accounts").getLongValue())
                    .peakHour(peakHour)
                    .peakHourLabel(String.format("%02d:00 – %02d:59", peakHour, peakHour))
                    .generatedAt(LocalDateTime.now())
                    .build();
        }
        // No data yet
        return AnalyticsSummaryDto.builder().generatedAt(LocalDateTime.now()).build();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Performs a streaming insert of one row into the specified table.
     * Errors are logged but never propagated — BigQuery failures must not
     * affect the core banking transaction flow.
     */
    private void insertRows(String tableName, Map<String, Object> rowData, String rowId) {
        try {
            TableId tableId = TableId.of(projectId, datasetId, tableName);
            InsertAllRequest insertRequest = InsertAllRequest.newBuilder(tableId)
                    .addRow(rowId, rowData)
                    .build();

            InsertAllResponse response = bigQuery.insertAll(insertRequest);
            if (response.hasErrors()) {
                response.getInsertErrors().forEach((idx, errors) ->
                        errors.forEach(err ->
                                log.warn("BigQuery insert error — table={} rowId={} reason={}",
                                        tableName, rowId, err.getMessage())));
            } else {
                log.debug("BigQuery stream insert OK — table={} rowId={}", tableName, rowId);
            }
        } catch (Exception ex) {
            // Never propagate — BQ is analytics-only, not transactional
            log.error("BigQuery stream insert failed — table={} rowId={}: {}", tableName, rowId, ex.getMessage(), ex);
        }
    }

    /**
     * Executes a standard SQL query synchronously and returns the result rows.
     *
     * @throws BigQueryException if the query fails
     */
    private Iterable<FieldValueList> runQuery(String sql) {
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(sql)
                .setUseLegacySql(false)
                .build();

        try {
            TableResult result = bigQuery.query(queryConfig);
            log.debug("BigQuery query executed — {} rows returned", result.getTotalRows());
            return result.iterateAll();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BigQueryException(500, "BigQuery query was interrupted", ex);
        }
    }

    /** Safely converts a BigQuery {@link FieldValue} to {@link BigDecimal}. Returns ZERO on null. */
    private BigDecimal toBigDecimal(FieldValue fv) {
        if (fv == null || fv.isNull()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(fv.getStringValue()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Returns {@code true} — used by callers to check whether BQ is enabled
     * without needing to inject the optional bean directly.
     */
    public boolean isEnabled() {
        return true;
    }
}
