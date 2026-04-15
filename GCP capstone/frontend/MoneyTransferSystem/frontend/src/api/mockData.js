/**
 * Mock data for demo/offline mode.
 *
 * Shapes mirror the real backend responses (report.md §5) so that the
 * rest of the frontend can use the same field names in both modes.
 *
 * Seed accounts (report.md §8):
 *   1  Alice Johnson  10000.00  ACTIVE
 *   2  Bob Smith       5000.00  ACTIVE
 *   3  Carol White     7500.00  ACTIVE
 *   4  David Brown     2500.00  LOCKED
 *   5  Eve Davis          0.00  CLOSED
 *
 * Default credentials in demo mode: any username + password "password"
 */
import { v4 as uuidv4 } from 'uuid';

// ── Accounts ──────────────────────────────────────────────────────────────────
// Shape: { id, holderName, balance, status, lastUpdated }
export const DEMO_ACCOUNTS = [
  { id: 1, holderName: 'Alice Johnson', balance: 10000.00, status: 'ACTIVE', lastUpdated: '2026-04-15T10:12:30.123' },
  { id: 2, holderName: 'Bob Smith',     balance: 5000.00,  status: 'ACTIVE', lastUpdated: '2026-04-15T10:12:30.123' },
  { id: 3, holderName: 'Carol White',   balance: 7500.00,  status: 'ACTIVE', lastUpdated: '2026-04-15T10:12:30.123' },
  { id: 4, holderName: 'David Brown',   balance: 2500.00,  status: 'LOCKED', lastUpdated: '2026-04-15T10:12:30.123' },
  { id: 5, holderName: 'Eve Davis',     balance: 0.00,     status: 'CLOSED', lastUpdated: '2026-04-15T10:12:30.123' },
];

export const DEMO_USER = {
  username:  'admin',
  accountId: 1,
  fullName:  'Alice Johnson',
};

// ── Transactions (Spring TransferResponse shape) ───────────────────────────────
// Shape: { transactionId, fromAccountId, toAccountId, amount, status,
//          failureReason, idempotencyKey, createdOn }
let _transactions = [
  {
    transactionId:  'a1b2c3d4-0001-0001-0001-000000000001',
    fromAccountId:  1,
    toAccountId:    2,
    amount:         500.00,
    status:         'SUCCESS',
    failureReason:  null,
    idempotencyKey: 'seed-tx-001',
    createdOn:      '2026-04-13T08:00:00',
  },
  {
    transactionId:  'a1b2c3d4-0002-0002-0002-000000000002',
    fromAccountId:  2,
    toAccountId:    1,
    amount:         200.00,
    status:         'SUCCESS',
    failureReason:  null,
    idempotencyKey: 'seed-tx-002',
    createdOn:      '2026-04-12T14:30:00',
  },
  {
    transactionId:  'a1b2c3d4-0003-0003-0003-000000000003',
    fromAccountId:  1,
    toAccountId:    3,
    amount:         750.00,
    status:         'SUCCESS',
    failureReason:  null,
    idempotencyKey: 'seed-tx-003',
    createdOn:      '2026-04-11T09:45:00',
  },
  {
    transactionId:  'a1b2c3d4-0004-0004-0004-000000000004',
    fromAccountId:  3,
    toAccountId:    1,
    amount:         1200.00,
    status:         'SUCCESS',
    failureReason:  null,
    idempotencyKey: 'seed-tx-004',
    createdOn:      '2026-04-10T11:00:00',
  },
  {
    transactionId:  'a1b2c3d4-0005-0005-0005-000000000005',
    fromAccountId:  1,
    toAccountId:    2,
    amount:         300.00,
    status:         'FAILED',
    failureReason:  'Insufficient balance',
    idempotencyKey: 'seed-tx-005',
    createdOn:      '2026-04-09T15:30:00',
  },
];

/** Returns a shallow copy sorted newest-first */
export const getMockTransactions = () =>
  [..._transactions].sort((a, b) => new Date(b.createdOn) - new Date(a.createdOn));

/**
 * Simulates a successful transfer and adds it to the mock ledger.
 * Returns a TransferResponse-shaped object.
 */
export const addMockTransfer = ({ fromAccountId, toAccountId, amount, idempotencyKey }) => {
  const newTxn = {
    transactionId:  uuidv4(),
    fromAccountId:  Number(fromAccountId),
    toAccountId:    Number(toAccountId),
    amount:         parseFloat(amount),
    status:         'SUCCESS',
    failureReason:  null,
    idempotencyKey: idempotencyKey || uuidv4(),
    createdOn:      new Date().toISOString().replace('Z', ''),
  };
  _transactions.unshift(newTxn);

  // Adjust mock balances
  const fromAcct = DEMO_ACCOUNTS.find((a) => a.id === Number(fromAccountId));
  const toAcct   = DEMO_ACCOUNTS.find((a) => a.id === Number(toAccountId));
  if (fromAcct) fromAcct.balance = +(fromAcct.balance - parseFloat(amount)).toFixed(2);
  if (toAcct)   toAcct.balance   = +(toAcct.balance   + parseFloat(amount)).toFixed(2);

  return newTxn;
};
