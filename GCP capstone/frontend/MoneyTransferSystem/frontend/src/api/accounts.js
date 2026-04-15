import axiosInstance from './axiosInstance';

/**
 * Account API calls — relative to /api/v1.
 *
 * Backend endpoints (see report.md §5.1):
 *   GET /accounts/{id}                     → full account details
 *   GET /accounts/{id}/balance             → { balance }
 *   GET /accounts/{id}/transactions        → Spring Page<TransferResponse>
 *
 * There is NO /accounts/summary endpoint.
 */

/** Fetches full account details: { id, holderName, balance, status, lastUpdated }. */
export const getAccountByIdApi = (accountId) =>
  axiosInstance.get(`/accounts/${accountId}`);

/** Fetches only the balance: { balance }. */
export const getAccountBalanceApi = (accountId) =>
  axiosInstance.get(`/accounts/${accountId}/balance`);

/**
 * Fetches paginated transaction history for an account (FR-07).
 * @param {string|number} accountId
 * @param {{ page?: number, size?: number }} params  – 0-based page index
 */
export const getAccountTransactionsApi = (accountId, params = {}) =>
  axiosInstance.get(`/accounts/${accountId}/transactions`, { params });
