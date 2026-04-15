/**
 * AuthService — login(), logout(), isAuthenticated(), getToken(), getUser()
 *
 * The Spring Boot backend uses stateless HTTP Basic Auth.
 * There is no /auth/login endpoint — credentials are validated by probing
 * a real protected resource (GET /accounts/{id}).
 *
 * Login flow (FR-08):
 *  1. Encode "username:password" as Base64.
 *  2. Probe GET /accounts/{accountId} with the encoded token.
 *  3. On 200  → store token + user (derived from account response).
 *  4. On 401  → reject with an "Invalid credentials" message.
 *  5. Network unreachable → fall back to demo mode.
 *
 * Account ID mapping (dev / seed data):
 *   "admin" → accountId 1 (Alice Johnson)
 *   "user"  → accountId 2 (Bob Smith)
 *   others  → accountId 1 (first active account)
 */
import axiosInstance from '../api/axiosInstance';
import { DEMO_USER, DEMO_ACCOUNTS } from '../api/mockData';

const TOKEN_KEY = 'token';
const USER_KEY  = 'user';

/** Maps known Spring Security usernames to seeded account IDs. */
const USERNAME_TO_ACCOUNT_ID = {
  admin: 1,
  user:  2,
};

const isNetworkError = (err) =>
  !err.response && (err.code === 'ERR_NETWORK' || err.code === 'ECONNREFUSED' || err.message === 'Network Error');

const AuthService = {
  /**
   * Validates credentials against the backend; falls back to demo mode when
   * the backend is unreachable.
   * @returns {{ success: boolean, user?: object, message?: string }}
   */
  async login(username, password) {
    if (!username.trim()) return { success: false, message: 'Username is required.' };
    if (!password)        return { success: false, message: 'Password is required.' };

    const basicToken = btoa(`${username}:${password}`);
    const accountId  = USERNAME_TO_ACCOUNT_ID[username.toLowerCase()] ?? 1;

    try {
      // Probe a real protected endpoint to validate the credentials (FR-08).
      const { data: accountData } = await axiosInstance.get(`/accounts/${accountId}`, {
        headers: { Authorization: `Basic ${basicToken}` },
      });

      const resolvedUser = {
        username,
        accountId,
        fullName: accountData.holderName || username,
      };

      localStorage.setItem(TOKEN_KEY, basicToken);
      localStorage.setItem(USER_KEY, JSON.stringify(resolvedUser));
      return { success: true, user: resolvedUser };
    } catch (err) {
      // ── Demo / offline fallback ─────────────────────────────────────────
      if (isNetworkError(err)) {
        if (password !== 'password') {
          return { success: false, message: 'Demo mode: use password "password" to log in.' };
        }
        const demoAccount = DEMO_ACCOUNTS[0];
        const demoUser = {
          ...DEMO_USER,
          username,
          accountId: demoAccount.id,
          fullName: demoAccount.holderName,
        };
        localStorage.setItem(TOKEN_KEY, basicToken);
        localStorage.setItem(USER_KEY, JSON.stringify(demoUser));
        return { success: true, user: demoUser };
      }
      // ── Real backend error (401 wrong credentials, 404 account, etc.) ────
      if (err.response?.status === 401) {
        return { success: false, message: 'Invalid credentials. Please try again.' };
      }
      const message = err.response?.data?.message || 'Login failed. Please try again.';
      return { success: false, message };
    }
  },

  logout() {
    // Stateless backend — no server-side session to invalidate.
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    return Promise.resolve();
  },

  isAuthenticated() {
    return Boolean(localStorage.getItem(TOKEN_KEY));
  },

  getToken() {
    return localStorage.getItem(TOKEN_KEY);
  },

  getUser() {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  },
};

export default AuthService;
