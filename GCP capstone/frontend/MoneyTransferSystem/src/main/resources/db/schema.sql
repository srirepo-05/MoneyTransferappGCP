-- ============================================================
-- Money Transfer System — PostgreSQL Schema
-- ============================================================

-- Drop tables in reverse dependency order (safe for re-runs)
DROP TABLE IF EXISTS transaction_logs;
DROP TABLE IF EXISTS accounts;

-- ============================================================
-- ACCOUNTS table
-- ============================================================
CREATE TABLE accounts (
    id           BIGSERIAL        PRIMARY KEY,
    holder_name  VARCHAR(255)     NOT NULL,
    balance      DECIMAL(18, 2)   NOT NULL DEFAULT 0.00,
    status       VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    version      INT              NOT NULL DEFAULT 0,
    last_updated TIMESTAMP,

    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'LOCKED', 'CLOSED'))
);

-- ============================================================
-- TRANSACTION_LOGS table
-- ============================================================
CREATE TABLE transaction_logs (
    id               VARCHAR(36)     PRIMARY KEY,
    from_account     BIGINT          NOT NULL REFERENCES accounts(id),
    to_account       BIGINT          NOT NULL REFERENCES accounts(id),
    amount           DECIMAL(18, 2)  NOT NULL,
    status           VARCHAR(20)     NOT NULL,
    failure_reason   VARCHAR(255),
    idempotency_key  VARCHAR(100)    NOT NULL UNIQUE,
    created_on       TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_amount_positive     CHECK (amount > 0),
    CONSTRAINT chk_tx_status           CHECK (status IN ('SUCCESS', 'FAILED')),
    CONSTRAINT chk_different_accounts  CHECK (from_account <> to_account)
);

-- Indexes for common query patterns
CREATE INDEX idx_tx_from_account ON transaction_logs(from_account);
CREATE INDEX idx_tx_to_account   ON transaction_logs(to_account);
CREATE INDEX idx_tx_created_on   ON transaction_logs(created_on DESC);
