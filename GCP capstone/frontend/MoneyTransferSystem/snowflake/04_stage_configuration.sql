-- ============================================================
-- Money Transfer DW — Step 04: Stage Configuration
-- Internal named stage for flat-file loads (CSV).
-- ============================================================

USE WAREHOUSE COMPUTE_WH;
USE DATABASE  MONEY_TRANSFER_DW;
USE SCHEMA    ANALYTICS;

-- -------------------------------------------------------
-- File Format: CSV  (matches PostgreSQL COPY CSV output)
-- -------------------------------------------------------
CREATE OR REPLACE FILE FORMAT CSV_FORMAT
    TYPE             = 'CSV'
    FIELD_DELIMITER  = ','
    RECORD_DELIMITER = '\n'
    SKIP_HEADER      = 1
    FIELD_OPTIONALLY_ENCLOSED_BY = '"'
    NULL_IF          = ('NULL', 'null', '')
    EMPTY_FIELD_AS_NULL = TRUE
    DATE_FORMAT      = 'YYYY-MM-DD'
    TIMESTAMP_FORMAT = 'YYYY-MM-DD HH24:MI:SS'
    COMMENT          = 'Standard CSV format for PostgreSQL exports';

-- -------------------------------------------------------
-- Internal Stage: accounts export
-- PUT file://path/to/accounts.csv @STG_ACCOUNTS;
-- -------------------------------------------------------
CREATE OR REPLACE STAGE STG_ACCOUNTS
    FILE_FORMAT = CSV_FORMAT
    COMMENT     = 'Staging area for accounts.csv exported from PostgreSQL';

-- -------------------------------------------------------
-- Internal Stage: transaction_logs export
-- PUT file://path/to/transaction_logs.csv @STG_TRANSACTIONS;
-- -------------------------------------------------------
CREATE OR REPLACE STAGE STG_TRANSACTIONS
    FILE_FORMAT = CSV_FORMAT
    COMMENT     = 'Staging area for transaction_logs.csv exported from PostgreSQL';

-- -------------------------------------------------------
-- Verification helpers
-- -------------------------------------------------------
-- LIST @STG_ACCOUNTS;
-- LIST @STG_TRANSACTIONS;
