-- ============================================================
-- Money Transfer System — BigQuery Setup Script
-- Run once per GCP project to create the dataset and tables.
--
-- Prerequisites:
--   - GCP project created with billing enabled
--   - BigQuery API enabled: gcloud services enable bigquery.googleapis.com
--   - Service account with roles/bigquery.dataEditor + roles/bigquery.jobUser
--
-- Usage (bq CLI):
--   bq mk --location=US money_transfer_analytics
--   bq query --use_legacy_sql=false < 01_bigquery_setup.sql
-- ============================================================


-- ============================================================
-- 1. Dataset
-- ============================================================
-- Create via CLI (datasets cannot be created with DDL in BigQuery):
--   bq mk --location=US --description="Money Transfer analytics dataset" \
--          YOUR_PROJECT_ID:money_transfer_analytics


-- ============================================================
-- 2. Transactions Table
--    Mirrors the MySQL transaction_logs table.
--    Populated via Streaming Insert after every transfer attempt.
-- ============================================================
CREATE TABLE IF NOT EXISTS `money_transfer_analytics.transactions`
(
    transaction_id  STRING    NOT NULL OPTIONS(description = 'UUID from MySQL transaction_logs.id'),
    from_account_id INT64     NOT NULL OPTIONS(description = 'Source account ID'),
    to_account_id   INT64     NOT NULL OPTIONS(description = 'Destination account ID'),
    amount          FLOAT64   NOT NULL OPTIONS(description = 'Transfer amount in USD'),
    status          STRING    NOT NULL OPTIONS(description = 'SUCCESS | FAILED'),
    failure_reason  STRING             OPTIONS(description = 'Non-null when status=FAILED'),
    idempotency_key STRING    NOT NULL OPTIONS(description = 'Client-supplied unique key'),
    created_on      TIMESTAMP NOT NULL OPTIONS(description = 'Transfer timestamp (UTC)')
)
PARTITION BY DATE(created_on)
CLUSTER BY status, from_account_id
OPTIONS(
    description = 'Money transfer events streamed from Spring Boot in real time',
    partition_expiration_days = 1825   -- 5 years retention
);


-- ============================================================
-- 3. Accounts Table
--    Account snapshots streamed after every balance change.
--    Allows point-in-time balance lookups in analytics.
-- ============================================================
CREATE TABLE IF NOT EXISTS `money_transfer_analytics.accounts`
(
    account_id  INT64     NOT NULL OPTIONS(description = 'MySQL accounts.id'),
    holder_name STRING    NOT NULL OPTIONS(description = 'Account holder full name'),
    balance     FLOAT64   NOT NULL OPTIONS(description = 'Balance at time of snapshot'),
    status      STRING    NOT NULL OPTIONS(description = 'ACTIVE | LOCKED | CLOSED'),
    snapshot_at TIMESTAMP NOT NULL OPTIONS(description = 'When this snapshot was taken (UTC)')
)
PARTITION BY DATE(snapshot_at)
CLUSTER BY account_id
OPTIONS(
    description = 'Account balance snapshots streamed from Spring Boot after each transfer'
);
