-- ============================================================
-- Money Transfer DW — Step 06: Analytics Queries
-- Business-intelligence queries against the dimensional model.
-- ============================================================

USE WAREHOUSE COMPUTE_WH;
USE DATABASE  MONEY_TRANSFER_DW;
USE SCHEMA    ANALYTICS;

-- ============================================================
-- Q1: Daily Transaction Volume
-- Count and total amount of transfers per calendar day.
-- ============================================================
SELECT
    d.full_date                                          AS transaction_date,
    d.day_name,
    COUNT(*)                                             AS total_transactions,
    COUNT(CASE WHEN f.status = 'SUCCESS' THEN 1 END)    AS successful_transactions,
    COUNT(CASE WHEN f.status = 'FAILED'  THEN 1 END)    AS failed_transactions,
    SUM(CASE WHEN f.status = 'SUCCESS' THEN f.amount ELSE 0 END) AS total_amount_transferred,
    ROUND(AVG(f.amount), 2)                              AS avg_transfer_amount
FROM FACT_TRANSACTIONS f
JOIN DIM_DATE d ON d.date_key = f.date_key
GROUP BY d.full_date, d.day_name
ORDER BY d.full_date DESC;


-- ============================================================
-- Q2: Account Activity — Most Active Accounts
-- Ranks accounts by total number of transfers (sent + received).
-- ============================================================
WITH account_totals AS (
    SELECT
        a.account_id,
        a.holder_name,
        a.status,
        COUNT(DISTINCT CASE WHEN f.account_from_key = a.account_key THEN f.transaction_key END) AS transfers_sent,
        COUNT(DISTINCT CASE WHEN f.account_to_key   = a.account_key THEN f.transaction_key END) AS transfers_received,
        COALESCE(SUM(CASE WHEN f.account_from_key = a.account_key AND f.status = 'SUCCESS' THEN f.amount END), 0) AS total_sent,
        COALESCE(SUM(CASE WHEN f.account_to_key   = a.account_key AND f.status = 'SUCCESS' THEN f.amount END), 0) AS total_received
    FROM DIM_ACCOUNT a
    LEFT JOIN FACT_TRANSACTIONS f
        ON f.account_from_key = a.account_key
        OR f.account_to_key   = a.account_key
    WHERE a.is_current = TRUE
    GROUP BY a.account_key, a.account_id, a.holder_name, a.status
)
SELECT
    account_id,
    holder_name,
    status,
    transfers_sent,
    transfers_received,
    transfers_sent + transfers_received  AS total_activity,
    total_sent,
    total_received,
    total_received - total_sent          AS net_flow,
    DENSE_RANK() OVER (ORDER BY transfers_sent + transfers_received DESC) AS activity_rank
FROM account_totals
ORDER BY activity_rank;


-- ============================================================
-- Q3: Success Rate — Percentage of Successful Transfers
-- Overall and broken down by month.
-- ============================================================
SELECT
    d.year,
    d.month,
    d.month_name,
    COUNT(*)                                                              AS total_transfers,
    COUNT(CASE WHEN f.status = 'SUCCESS' THEN 1 END)                     AS successful,
    COUNT(CASE WHEN f.status = 'FAILED'  THEN 1 END)                     AS failed,
    ROUND(
        COUNT(CASE WHEN f.status = 'SUCCESS' THEN 1 END) * 100.0
        / NULLIF(COUNT(*), 0),
    2)                                                                    AS success_rate_pct,
    SUM(CASE WHEN f.status = 'SUCCESS' THEN f.amount ELSE 0 END)         AS volume_transferred
FROM FACT_TRANSACTIONS f
JOIN DIM_DATE d ON d.date_key = f.date_key
GROUP BY d.year, d.month, d.month_name
ORDER BY d.year, d.month;


-- ============================================================
-- Q4: Peak Hours — Busiest Transaction Times
-- Identifies which hours of the day have the most transfers.
-- ============================================================
SELECT
    f.hour_of_day,
    LPAD(f.hour_of_day::VARCHAR, 2, '0') || ':00'           AS hour_label,
    COUNT(*)                                                 AS total_transfers,
    COUNT(CASE WHEN f.status = 'SUCCESS' THEN 1 END)        AS successful,
    ROUND(AVG(f.amount), 2)                                  AS avg_amount,
    SUM(CASE WHEN f.status = 'SUCCESS' THEN f.amount END)   AS total_volume,
    RANK() OVER (ORDER BY COUNT(*) DESC)                     AS peak_rank
FROM FACT_TRANSACTIONS f
GROUP BY f.hour_of_day
ORDER BY f.hour_of_day;


-- ============================================================
-- Q5: Average Transfer Amount
-- Mean, median, min, max — overall and by status.
-- ============================================================
SELECT
    status,
    COUNT(*)                          AS transfer_count,
    ROUND(AVG(amount),    2)          AS avg_amount,
    ROUND(MEDIAN(amount), 2)          AS median_amount,
    MIN(amount)                       AS min_amount,
    MAX(amount)                       AS max_amount,
    ROUND(STDDEV(amount), 2)          AS stddev_amount,
    SUM(amount)                       AS total_amount
FROM FACT_TRANSACTIONS
GROUP BY status

UNION ALL

SELECT
    'ALL'                             AS status,
    COUNT(*)                          AS transfer_count,
    ROUND(AVG(amount),    2)          AS avg_amount,
    ROUND(MEDIAN(amount), 2)          AS median_amount,
    MIN(amount)                       AS min_amount,
    MAX(amount)                       AS max_amount,
    ROUND(STDDEV(amount), 2)          AS stddev_amount,
    SUM(amount)                       AS total_amount
FROM FACT_TRANSACTIONS

ORDER BY status;


-- ============================================================
-- Q6: Quarterly Summary (bonus — executive dashboard)
-- ============================================================
SELECT
    d.year,
    d.quarter,
    d.quarter_label,
    COUNT(*)                                                               AS total_transfers,
    COUNT(CASE WHEN f.status = 'SUCCESS' THEN 1 END)                      AS successful,
    ROUND(
        COUNT(CASE WHEN f.status = 'SUCCESS' THEN 1 END) * 100.0
        / NULLIF(COUNT(*), 0),
    2)                                                                     AS success_rate_pct,
    SUM(CASE WHEN f.status = 'SUCCESS' THEN f.amount ELSE 0 END)          AS quarterly_volume,
    ROUND(AVG(f.amount), 2)                                                AS avg_transfer_amount,
    COUNT(DISTINCT f.account_from_key)                                     AS unique_senders
FROM FACT_TRANSACTIONS f
JOIN DIM_DATE d ON d.date_key = f.date_key
GROUP BY d.year, d.quarter, d.quarter_label
ORDER BY d.year, d.quarter;
