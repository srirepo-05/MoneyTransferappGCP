-- ============================================================
-- Money Transfer DW — Step 03: Fact Table
-- FACT_TRANSACTIONS
-- ============================================================

USE WAREHOUSE COMPUTE_WH;
USE DATABASE  MONEY_TRANSFER_DW;
USE SCHEMA    ANALYTICS;

-- -------------------------------------------------------
-- FACT_TRANSACTIONS
-- Grain: one row per transaction log entry.
-- -------------------------------------------------------
CREATE OR REPLACE TABLE FACT_TRANSACTIONS (
    transaction_key   VARCHAR(36)    NOT NULL PRIMARY KEY    COMMENT 'Natural key — maps to transaction_logs.id (UUID) in PostgreSQL',
    account_from_key  NUMBER         NOT NULL                COMMENT 'FK → DIM_ACCOUNT.account_key (sender)',
    account_to_key    NUMBER         NOT NULL                COMMENT 'FK → DIM_ACCOUNT.account_key (receiver)',
    date_key          NUMBER(8)      NOT NULL                COMMENT 'FK → DIM_DATE.date_key (YYYYMMDD)',
    hour_of_day       NUMBER(2)      NOT NULL                COMMENT '0-23, derived from created_on',
    amount            DECIMAL(18,2)  NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    failure_reason    VARCHAR(255),
    idempotency_key   VARCHAR(100)   NOT NULL UNIQUE,
    created_on        TIMESTAMP_NTZ  NOT NULL,

    -- Referential integrity (enforced in ETL, not by Snowflake constraint by default)
    CONSTRAINT fk_fact_account_from FOREIGN KEY (account_from_key) REFERENCES DIM_ACCOUNT(account_key) NOT ENFORCED,
    CONSTRAINT fk_fact_account_to   FOREIGN KEY (account_to_key)   REFERENCES DIM_ACCOUNT(account_key) NOT ENFORCED,
    CONSTRAINT fk_fact_date         FOREIGN KEY (date_key)         REFERENCES DIM_DATE(date_key)        NOT ENFORCED
)
COMMENT = 'Fact: one row per money transfer attempt';

-- Cluster key — improves pruning for date-range analytics
ALTER TABLE FACT_TRANSACTIONS CLUSTER BY (date_key, status);
