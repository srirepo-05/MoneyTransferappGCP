-- ============================================================
-- Money Transfer System — BigQuery Analytics Queries
-- Mirror of Snowflake analytics queries, rewritten in BigQuery
-- Standard SQL dialect.
-- ============================================================


-- ============================================================
-- Q1: Daily Transaction Volume
-- Count and total amount of transfers per calendar day.
-- ============================================================
SELECT
    DATE(created_on)                                                    AS transaction_date,
    FORMAT_DATE('%A', DATE(created_on))                                 AS day_name,
    COUNT(*)                                                            AS total_transactions,
    COUNTIF(status = 'SUCCESS')                                         AS successful_transactions,
    COUNTIF(status = 'FAILED')                                          AS failed_transactions,
    COALESCE(SUM(IF(status = 'SUCCESS', amount, 0)), 0)                 AS total_amount_transferred,
    ROUND(AVG(amount), 2)                                               AS avg_transfer_amount
FROM `money_transfer_analytics.transactions`
GROUP BY transaction_date, day_name
ORDER BY transaction_date DESC;


-- ============================================================
-- Q2: Account Activity — Most Active Accounts
-- Ranks accounts by total number of transfers (sent + received).
-- ============================================================
WITH account_totals AS (
    SELECT
        a.account_id,
        a.holder_name,
        a.status,
        COUNTIF(t.from_account_id = a.account_id)                                              AS transfers_sent,
        COUNTIF(t.to_account_id   = a.account_id)                                              AS transfers_received,
        COALESCE(SUM(IF(t.from_account_id = a.account_id AND t.status = 'SUCCESS', t.amount, 0)), 0) AS total_sent,
        COALESCE(SUM(IF(t.to_account_id   = a.account_id AND t.status = 'SUCCESS', t.amount, 0)), 0) AS total_received
    FROM (
        SELECT DISTINCT account_id, holder_name, status
        FROM `money_transfer_analytics.accounts`
    ) a
    LEFT JOIN `money_transfer_analytics.transactions` t
        ON t.from_account_id = a.account_id
        OR t.to_account_id   = a.account_id
    GROUP BY a.account_id, a.holder_name, a.status
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
ORDER BY activity_rank;


-- ============================================================
-- Q3: Success Rate
-- Percentage of successful vs failed transfers overall.
-- ============================================================
SELECT
    COUNT(*)                                                                AS total_transactions,
    COUNTIF(status = 'SUCCESS')                                             AS successful_transactions,
    COUNTIF(status = 'FAILED')                                              AS failed_transactions,
    ROUND(SAFE_DIVIDE(COUNTIF(status = 'SUCCESS') * 100.0, COUNT(*)), 2)   AS success_rate_percent,
    COALESCE(SUM(IF(status = 'SUCCESS', amount, 0)), 0)                    AS total_volume_transferred,
    ROUND(AVG(IF(status = 'SUCCESS', amount, NULL)), 2)                    AS avg_successful_amount
FROM `money_transfer_analytics.transactions`;


-- ============================================================
-- Q4: Peak Hours — Busiest Transaction Hours
-- Ranks each hour of day (0–23 UTC) by transfer count.
-- ============================================================
SELECT
    EXTRACT(HOUR FROM created_on)                   AS hour_of_day,
    COUNT(*)                                         AS total_transactions,
    COALESCE(SUM(amount), 0)                         AS total_amount,
    ROUND(AVG(amount), 2)                            AS avg_amount,
    DENSE_RANK() OVER (ORDER BY COUNT(*) DESC)       AS rank
FROM `money_transfer_analytics.transactions`
GROUP BY hour_of_day
ORDER BY rank;


-- ============================================================
-- Q5: Average Transfer Amount
-- Overall and broken down by account and status.
-- ============================================================
SELECT
    ROUND(AVG(amount), 2)                                               AS overall_avg,
    ROUND(AVG(IF(status = 'SUCCESS', amount, NULL)), 2)                 AS avg_successful,
    ROUND(AVG(IF(status = 'FAILED',  amount, NULL)), 2)                 AS avg_failed,
    MAX(IF(status = 'SUCCESS', amount, NULL))                           AS max_transfer,
    MIN(IF(status = 'SUCCESS', amount, NULL))                           AS min_transfer
FROM `money_transfer_analytics.transactions`;
