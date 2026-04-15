-- ============================================================
-- Money Transfer System — Seed Data (PostgreSQL)
-- ============================================================

-- Clear existing data (development / demo only)
TRUNCATE TABLE transaction_logs, accounts RESTART IDENTITY CASCADE;

-- ============================================================
-- Sample Accounts
-- ============================================================
INSERT INTO accounts (holder_name, balance, status, version, last_updated) VALUES
    ('Alice Johnson',  10000.00, 'ACTIVE', 0, NOW()),
    ('Bob Smith',       5000.00, 'ACTIVE', 0, NOW()),
    ('Carol White',     7500.00, 'ACTIVE', 0, NOW()),
    ('David Brown',     2500.00, 'LOCKED', 0, NOW()),
    ('Eve Davis',          0.00, 'CLOSED', 0, NOW());

-- ============================================================
-- Sample Transaction Logs
-- ============================================================
INSERT INTO transaction_logs
    (id, from_account, to_account, amount, status, failure_reason, idempotency_key, created_on)
VALUES
    ('a1b2c3d4-0001-0001-0001-000000000001',
     1, 2, 500.00, 'SUCCESS', NULL,
     'seed-tx-001', NOW() - INTERVAL '2 days'),

    ('a1b2c3d4-0002-0002-0002-000000000002',
     2, 3, 200.00, 'SUCCESS', NULL,
     'seed-tx-002', NOW() - INTERVAL '1 day'),

    ('a1b2c3d4-0003-0003-0003-000000000003',
     1, 3, 9999.00, 'FAILED', 'Insufficient balance on account 1. Available: 9500.00, Requested: 9999.00',
     'seed-tx-003', NOW() - INTERVAL '12 hours');
