-- ============================================================
-- Money Transfer — PostgreSQL Export Helper
-- Run these commands against your PostgreSQL database to
-- produce the CSV files needed by the Snowflake ETL.
--
-- Usage (psql):
--   psql -U postgres -d moneytransfer -f export_to_csv.sql
-- ============================================================

-- Export accounts table
\COPY (
    SELECT
        id,
        holder_name,
        balance,
        status,
        version,
        TO_CHAR(last_updated, 'YYYY-MM-DD HH24:MI:SS') AS last_updated
    FROM accounts
    ORDER BY id
) TO 'exports/accounts.csv'
WITH (FORMAT CSV, HEADER TRUE, NULL 'NULL');

-- Export transaction_logs table
\COPY (
    SELECT
        id,
        from_account,
        to_account,
        amount,
        status,
        COALESCE(failure_reason, 'NULL')              AS failure_reason,
        idempotency_key,
        TO_CHAR(created_on, 'YYYY-MM-DD HH24:MI:SS')  AS created_on
    FROM transaction_logs
    ORDER BY created_on
) TO 'exports/transaction_logs.csv'
WITH (FORMAT CSV, HEADER TRUE, NULL 'NULL');
