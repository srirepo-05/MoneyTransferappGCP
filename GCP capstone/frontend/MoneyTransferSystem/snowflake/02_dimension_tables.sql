-- ============================================================
-- Money Transfer DW — Step 02: Dimension Tables
-- DIM_ACCOUNT  |  DIM_DATE
-- ============================================================

USE WAREHOUSE COMPUTE_WH;
USE DATABASE  MONEY_TRANSFER_DW;
USE SCHEMA    ANALYTICS;

-- -------------------------------------------------------
-- DIM_ACCOUNT
-- Slowly Changing Dimension Type 2 — one row per version
-- of each account (tracks status changes over time).
-- -------------------------------------------------------
CREATE OR REPLACE TABLE DIM_ACCOUNT (
    account_key      NUMBER        NOT NULL AUTOINCREMENT PRIMARY KEY,
    account_id       NUMBER(19)    NOT NULL               COMMENT 'Natural key — maps to accounts.id in PostgreSQL',
    holder_name      VARCHAR(255)  NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    effective_date   DATE          NOT NULL               COMMENT 'Date this version became effective',
    expiry_date      DATE                                 COMMENT 'NULL = current record',
    is_current       BOOLEAN       NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_dim_account_natural UNIQUE (account_id, effective_date)
)
COMMENT = 'Dimension: account master data (SCD Type 2)';

-- -------------------------------------------------------
-- DIM_DATE
-- Pre-populated date spine — one row per calendar date.
-- Populate with the stored procedure below.
-- -------------------------------------------------------
CREATE OR REPLACE TABLE DIM_DATE (
    date_key         NUMBER(8)     NOT NULL PRIMARY KEY   COMMENT 'Surrogate key in YYYYMMDD format',
    full_date        DATE          NOT NULL UNIQUE,
    day              NUMBER(2)     NOT NULL,
    month            NUMBER(2)     NOT NULL,
    month_name       VARCHAR(10)   NOT NULL,
    year             NUMBER(4)     NOT NULL,
    quarter          NUMBER(1)     NOT NULL               COMMENT '1-4',
    quarter_label    VARCHAR(6)    NOT NULL               COMMENT 'e.g. Q1-2025',
    day_of_week      NUMBER(1)     NOT NULL               COMMENT '0=Sun … 6=Sat',
    day_name         VARCHAR(10)   NOT NULL,
    is_weekend       BOOLEAN       NOT NULL,
    week_of_year     NUMBER(2)     NOT NULL
)
COMMENT = 'Dimension: calendar date spine';

-- -------------------------------------------------------
-- Stored procedure: populate DIM_DATE for a date range
-- Usage: CALL POPULATE_DIM_DATE('2024-01-01', '2030-12-31');
-- -------------------------------------------------------
CREATE OR REPLACE PROCEDURE POPULATE_DIM_DATE(start_date VARCHAR, end_date VARCHAR)
RETURNS VARCHAR
LANGUAGE JAVASCRIPT
AS
$$
    var sql = `
        INSERT INTO DIM_DATE
        WITH RECURSIVE date_spine AS (
            SELECT TO_DATE(:1)  AS d
            UNION ALL
            SELECT DATEADD('day', 1, d) FROM date_spine WHERE d < TO_DATE(:2)
        )
        SELECT
            TO_NUMBER(TO_CHAR(d, 'YYYYMMDD'))           AS date_key,
            d                                           AS full_date,
            DAY(d)                                      AS day,
            MONTH(d)                                    AS month,
            TO_CHAR(d, 'MMMM')                         AS month_name,
            YEAR(d)                                     AS year,
            QUARTER(d)                                  AS quarter,
            'Q' || QUARTER(d) || '-' || YEAR(d)        AS quarter_label,
            DAYOFWEEK(d)                                AS day_of_week,
            DAYNAME(d)                                  AS day_name,
            DAYOFWEEK(d) IN (0, 6)                      AS is_weekend,
            WEEKOFYEAR(d)                               AS week_of_year
        FROM date_spine;
    `;
    var stmt = snowflake.createStatement({ sqlText: sql, binds: [START_DATE, END_DATE] });
    var res  = stmt.execute();
    return 'DIM_DATE populated from ' + START_DATE + ' to ' + END_DATE;
$$;

-- Populate DIM_DATE for 2024-01-01 → 2030-12-31
CALL POPULATE_DIM_DATE('2024-01-01', '2030-12-31');
