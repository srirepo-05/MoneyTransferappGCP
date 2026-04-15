# BigQuery Integration — Money Transfer System

## Overview

Every successful (and failed) fund transfer is **streamed to Google BigQuery in real time** by the Spring Boot backend immediately after the MySQL transaction commits. This provides a live analytics data warehouse without any batch ETL jobs.

---

## Architecture

```
User → POST /api/v1/transfers
         │
         ▼
    TransferController
         │
         ▼
    TransferService  ──────@Transactional──────►  MySQL
         │                                         (transaction_logs, accounts)
         │   (fire-and-forget @Async)
         ▼
    BigQueryService.streamTransaction()
    BigQueryService.streamAccount(from)
    BigQueryService.streamAccount(to)
         │
         ▼
    BigQuery Streaming Insert API
         │
         ▼
    money_transfer_analytics.transactions   (partitioned by DATE(created_on))
    money_transfer_analytics.accounts       (partitioned by DATE(snapshot_at))
```

BigQuery is **analytics-only** — it never affects the banking transaction. If BigQuery is unreachable, the error is logged and the HTTP response is unaffected.

---

## GCP Setup (one-time)

### 1. Enable BigQuery API

```bash
gcloud services enable bigquery.googleapis.com --project=YOUR_PROJECT_ID
```

### 2. Create the dataset

```bash
bq mk \
  --location=US \
  --description="Money Transfer analytics dataset" \
  YOUR_PROJECT_ID:money_transfer_analytics
```

### 3. Create the tables

```bash
bq query --use_legacy_sql=false < bigquery/01_bigquery_setup.sql
```

### 4. Create a service account and key

```bash
# Create service account
gcloud iam service-accounts create money-transfer-bq-sa \
  --display-name="Money Transfer BigQuery SA" \
  --project=YOUR_PROJECT_ID

# Grant BigQuery permissions
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:money-transfer-bq-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/bigquery.dataEditor"

gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:money-transfer-bq-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/bigquery.jobUser"

# Download key
gcloud iam service-accounts keys create ~/.gcp/money-transfer-sa.json \
  --iam-account=money-transfer-bq-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com
```

---

## Spring Boot Configuration

Edit `src/main/resources/application.properties`:

```properties
# Enable BigQuery
bigquery.enabled=true
bigquery.project-id=YOUR_GCP_PROJECT_ID
bigquery.dataset-id=money_transfer_analytics

# Option A — Service account key file (local dev)
bigquery.credentials-file=/path/to/money-transfer-sa.json

# Option B — Application Default Credentials (GCP-hosted / CI)
# bigquery.credentials-file=          ← leave blank
```

**On GCP (Cloud Run, GKE, App Engine):** leave `bigquery.credentials-file` blank — the workload identity / attached service account is used automatically.

---

## REST API — Analytics Endpoints

All endpoints require `ADMIN` role (HTTP Basic: `admin` / `admin`).

| Method | Path | Query Params | Description |
|--------|------|--------------|-------------|
| GET | `/api/v1/analytics/summary` | — | All KPIs in one call |
| GET | `/api/v1/analytics/daily-volume` | `days=30` | Daily counts & totals |
| GET | `/api/v1/analytics/success-rate` | — | Overall success/failure rate |
| GET | `/api/v1/analytics/top-accounts` | `limit=10` | Most active accounts |
| GET | `/api/v1/analytics/peak-hours` | `top=5` | Busiest hours of day |

### Example — summary

```bash
curl -u admin:admin http://localhost:8080/api/v1/analytics/summary
```

```json
{
  "totalTransactions": 120,
  "successfulTransactions": 115,
  "failedTransactions": 5,
  "successRatePercent": 95.83,
  "totalVolumeTransferred": 48250.00,
  "avgTransferAmount": 419.57,
  "maxTransferAmount": 5000.00,
  "minTransferAmount": 10.00,
  "activeAccounts": 5,
  "peakHour": 14,
  "peakHourLabel": "14:00 – 14:59",
  "generatedAt": "2026-04-15T11:30:00"
}
```

### Example — daily volume

```bash
curl -u admin:admin "http://localhost:8080/api/v1/analytics/daily-volume?days=7"
```

### Example — top accounts

```bash
curl -u admin:admin "http://localhost:8080/api/v1/analytics/top-accounts?limit=5"
```

---

## When BigQuery is Disabled

Set `bigquery.enabled=false` (default for local dev). Analytics endpoints return:

```json
{
  "error": "BIGQUERY_DISABLED",
  "message": "BigQuery integration is not enabled. Set bigquery.enabled=true and provide GCP credentials to activate analytics."
}
```

The core banking API (`/api/v1/accounts`, `/api/v1/transfers`) works fully without BigQuery.

---

## Table Schemas

### `transactions`

| Column | Type | Description |
|--------|------|-------------|
| `transaction_id` | STRING | UUID from MySQL |
| `from_account_id` | INT64 | Sender account ID |
| `to_account_id` | INT64 | Receiver account ID |
| `amount` | FLOAT64 | Transfer amount |
| `status` | STRING | `SUCCESS` or `FAILED` |
| `failure_reason` | STRING | Error message (nullable) |
| `idempotency_key` | STRING | Client-supplied unique key |
| `created_on` | TIMESTAMP | Transfer time (UTC) |

Partitioned by `DATE(created_on)`, clustered by `(status, from_account_id)`.

### `accounts`

| Column | Type | Description |
|--------|------|-------------|
| `account_id` | INT64 | MySQL account ID |
| `holder_name` | STRING | Account holder name |
| `balance` | FLOAT64 | Balance at snapshot time |
| `status` | STRING | `ACTIVE`, `LOCKED`, or `CLOSED` |
| `snapshot_at` | TIMESTAMP | When snapshot was taken |

Partitioned by `DATE(snapshot_at)`, clustered by `account_id`.

---

## Files

| File | Purpose |
|------|---------|
| `bigquery/01_bigquery_setup.sql` | DDL to create tables |
| `bigquery/02_analytics_queries.sql` | All 5 analytics SQL queries |
| `bigquery/README.md` | This file |
| `src/.../config/BigQueryConfig.java` | Spring `BigQuery` bean |
| `src/.../service/BigQueryService.java` | Streaming inserts + query methods |
| `src/.../controller/AnalyticsController.java` | REST endpoints |
| `src/.../domain/dto/analytics/` | Response DTOs |
