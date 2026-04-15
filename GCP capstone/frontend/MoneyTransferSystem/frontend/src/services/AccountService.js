/**
 * AccountService — getAccount(), getBalance(), getTransactions()
 *
 * Backend endpoints (report.md §5.1):
 *   GET /accounts/{id}                      → { id, holderName, balance, status, lastUpdated }
 *   GET /accounts/{id}/balance              → { balance }
 *   GET /accounts/{id}/transactions         → Spring Page<TransferResponse>
 *       page (0-based), size (default 20)
 *
 * Falls back to in-memory mock data when the backend is not reachable.
 */
import axiosInstance from '../api/axiosInstance';
import { DEMO_ACCOUNTS, getMockTransactions } from '../api/mockData';

const isNetworkError = (err) =>
  !err.response && (err.code === 'ERR_NETWORK' || err.code === 'ECONNREFUSED' || err.message === 'Network Error');

const AccountService = {
  /**
   * Fetches full account details for a given ID.
   * Response shape: { id, holderName, balance, status, lastUpdated }
   */
  async getAccount(accountId) {
    try {
      const { data } = await axiosInstance.get(`/accounts/${accountId}`);
      return data;
    } catch (err) {
      if (isNetworkError(err)) {
        return DEMO_ACCOUNTS.find((a) => String(a.id) === String(accountId)) || DEMO_ACCOUNTS[0];
      }
      throw err;
    }
  },

  /**
   * Returns only the balance for a specific account.
   * @returns {Promise<number>}
   */
  async getBalance(accountId) {
    try {
      const { data } = await axiosInstance.get(`/accounts/${accountId}/balance`);
      return data.balance ?? data;
    } catch (err) {
      if (isNetworkError(err)) {
        const acct = DEMO_ACCOUNTS.find((a) => String(a.id) === String(accountId)) || DEMO_ACCOUNTS[0];
        return acct.balance;
      }
      throw err;
    }
  },

  /**
   * Returns a Spring Page of transactions for an account (FR-07).
   *
   * @param {string|number} accountId
   * @param {{ page?: number, size?: number }} params  — page is 0-based
   * @returns {Promise<{ content, totalElements, totalPages, number, size, first, last, empty }>}
   */
  async getTransactions(accountId, params = {}) {
    // Ensure safe defaults; backend is 0-based
    const { page = 0, size = 10 } = params;
    try {
      const { data } = await axiosInstance.get(`/accounts/${accountId}/transactions`, {
        params: { page, size },
      });
      // data is Spring Page — return as-is so callers use data.content etc.
      return data;
    } catch (err) {
      if (isNetworkError(err)) {
        const allTxns = getMockTransactions();
        const start   = page * size;
        const content = allTxns.slice(start, start + size);
        return {
          content,
          totalElements: allTxns.length,
          totalPages: Math.max(1, Math.ceil(allTxns.length / size)),
          number: page,
          size,
          first: page === 0,
          last: start + size >= allTxns.length,
          empty: content.length === 0,
        };
      }
      throw err;
    }
  },
};

export default AccountService;
