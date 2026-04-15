-- ============================================================
-- Money Transfer DW — Step 05: ETL Pipeline
-- Load PostgreSQL exports into the dimensional model.
--
-- Execution order:
--   1. Load DIM_ACCOUNT   (from @STG_ACCOUNTS)
--   2. Load FACT_TRANSACTIONS (from @STG_TRANSACTIONS)
--
-- Pre-requisites:
--   • PostgreSQL CSV exports uploaded to internal stages:
--       PUT file://exports/accounts.csv         @STG_ACCOUNTS;
--       PUT file://exports/transaction_logs.csv  @STG_TRANSACTIONS;
--   • DIM_DATE already populated (02_dimension_tables.sql)
-- ============================================================

USE WAREHOUSE COMPUTE_WH;
USE DATABASE  MONEY_TRANSFER_DW;
USE SCHEMA    ANALYTICS;

-- ============================================================
-- STEP A — Raw staging tables (transient, no Fail-safe cost)
-- ============================================================

-- Raw accounts
CREATE OR REPLACE TRANSIENT TABLE STG_RAW_ACCOUNTS (
    id           NUMBER(19),
    holder_name  VARCHAR(255),
    balance      DECIMAL(18,2),
    status       VARCHAR(20),
    version      NUMBER,
    last_updated TIMESTAMP_NTZ
);

-- Raw transaction logs
CREATE OR REPLACE TRANSIENT TABLE STG_RAW_TRANSACTIONS (
    id               VARCHAR(36),
    from_account     NUMBER(19),
    to_account       NUMBER(19),
    amount           DECIMAL(18,2),
    status           VARCHAR(20),
    failure_reason   VARCHAR(255),
    idempotency_key  VARCHAR(100),
    created_on       TIMESTAMP_NTZ
);

-- ============================================================
-- STEP B — COPY INTO raw staging tables
-- ============================================================

-- Load accounts from internal stage
COPY INTO STG_RAW_ACCOUNTS (id, holder_name, balance, status, version, last_updated)
FROM (
    SELECT
        $1::NUMBER(19),
        $2::VARCHAR(255),
        $3::DECIMAL(18,2),
        $4::VARCHAR(20),
        $5::NUMBER,
        $6::TIMESTAMP_NTZ
    FROM @STG_ACCOUNTS
)
FILE_FORMAT = (FORMAT_NAME = 'CSV_FORMAT')
ON_ERROR    = 'ABORT_STATEMENT'
PURGE       = FALSE;

-- Load transaction logs from internal stage
COPY INTO STG_RAW_TRANSACTIONS (id, from_account, to_account, amount, status, failure_reason, idempotency_key, created_on)
FROM (
    SELECT
        $1::VARCHAR(36),
        $2::NUMBER(19),
        $3::NUMBER(19),
        $4::DECIMAL(18,2),
        $5::VARCHAR(20),
        $6::VARCHAR(255),
        $7::VARCHAR(100),
        $8::TIMESTAMP_NTZ
    FROM @STG_TRANSACTIONS
)
FILE_FORMAT = (FORMAT_NAME = 'CSV_FORMAT')
ON_ERROR    = 'ABORT_STATEMENT'
PURGE       = FALSE;

-- ============================================================
-- STEP C — Load DIM_ACCOUNT (upsert / SCD Type 1 for demo)
-- For a full SCD2 implementation see the comments below.
-- ============================================================
MERGE INTO DIM_ACCOUNT AS tgt
USING (
    SELECT
        id           AS account_id,
        holder_name,
        status,
        COALESCE(CAST(last_updated AS DATE), CURRENT_DATE) AS effective_date
    FROM STG_RAW_ACCOUNTS
) AS src
ON tgt.account_id = src.account_id AND tgt.is_current = TRUE
WHEN MATCHED AND tgt.status <> src.status THEN
    -- Status changed — expire old record
    UPDATE SET
        tgt.expiry_date = CURRENT_DATE,
        tgt.is_current  = FALSE
WHEN NOT MATCHED THEN
    INSERT (account_id, holder_name, status, effective_date, expiry_date, is_current)
    VALUES (src.account_id, src.holder_name, src.status, src.effective_date, NULL, TRUE);

-- Insert new current record for accounts whose status changed (SCD2 new version)
INSERT INTO DIM_ACCOUNT (account_id, holder_name, status, effective_date, expiry_date, is_current)
SELECT
    s.id,
    s.holder_name,
    s.status,
    CURRENT_DATE,
    NULL,
    TRUE
FROM STG_RAW_ACCOUNTS s
JOIN DIM_ACCOUNT d
    ON d.account_id = s.id
   AND d.is_current  = FALSE
   AND d.expiry_date = CURRENT_DATE  -- just expired this run
WHERE NOT EXISTS (
    SELECT 1 FROM DIM_ACCOUNT x
    WHERE x.account_id = s.id AND x.is_current = TRUE
);

-- ============================================================
-- STEP D — Load FACT_TRANSACTIONS  (INSERT new rows only)
-- ============================================================
INSERT INTO FACT_TRANSACTIONS (
    transaction_key,
    account_from_key,
    account_to_key,
    date_key,
    hour_of_day,
    amount,
    status,
    failure_reason,
    idempotency_key,
    created_on
)
SELECT
    t.id                                              AS transaction_key,
    da_from.account_key                               AS account_from_key,
    da_to.account_key                                 AS account_to_key,
    TO_NUMBER(TO_CHAR(t.created_on, 'YYYYMMDD'))      AS date_key,
    HOUR(t.created_on)                                AS hour_of_day,
    t.amount,
    t.status,
    t.failure_reason,
    t.idempotency_key,
    t.created_on
FROM STG_RAW_TRANSACTIONS t
-- resolve sender dimension key (current record)
JOIN DIM_ACCOUNT da_from
    ON da_from.account_id = t.from_account
   AND da_from.is_current  = TRUE
-- resolve receiver dimension key (current record)
JOIN DIM_ACCOUNT da_to
    ON da_to.account_id = t.to_account
   AND da_to.is_current  = TRUE
-- skip rows already loaded (idempotent re-runs)
WHERE NOT EXISTS (
    SELECT 1 FROM FACT_TRANSACTIONS f
    WHERE f.transaction_key = t.id
);

-- ============================================================
-- STEP E — Validation row counts
-- ============================================================
SELECT 'STG_RAW_ACCOUNTS'    AS tbl, COUNT(*) AS rows FROM STG_RAW_ACCOUNTS
UNION ALL
SELECT 'STG_RAW_TRANSACTIONS',        COUNT(*)         FROM STG_RAW_TRANSACTIONS
UNION ALL
SELECT 'DIM_ACCOUNT',                 COUNT(*)         FROM DIM_ACCOUNT
UNION ALL
SELECT 'FACT_TRANSACTIONS',           COUNT(*)         FROM FACT_TRANSACTIONS;
