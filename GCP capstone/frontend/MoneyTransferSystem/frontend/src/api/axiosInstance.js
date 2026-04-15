/**
 * Axios instance — src/api/axiosInstance.js
 *
 * Auth strategy  : HTTP Basic (Base64 "username:password" stored in localStorage as 'token')
 * Base URL       : /api/v1  — proxied to http://localhost:8080 by the Vite dev server to
 *                  avoid CORS preflight issues (see vite.config.js).
 * Interceptors   :
 *   - Request  : attaches Authorization: Basic <token> header
 *   - Response : 401 → clear credentials & redirect to /login
 *                409 → surface backend error code (DUPLICATE_TRANSFER,
 *                      INSUFFICIENT_BALANCE, ACCOUNT_NOT_ACTIVE, …)
 */
import axios from 'axios';

const axiosInstance = axios.create({
  // Relative path so that the Vite proxy can forward the request to localhost:8080.
  baseURL: '/api/v1',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

// ─── Request Interceptor ─────────────────────────────────────────────────────
// Reads the Base64-encoded "username:password" token saved during login and
// attaches it as an HTTP Basic Authorization header (FR-08).
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token'); // Base64 encoded 'user:pass'
    if (token) {
      config.headers.Authorization = `Basic ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ─── Response Interceptor ────────────────────────────────────────────────────
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    if (status === 401) {
      // Session expired or invalid credentials — clear state and go to login
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }

    if (status === 409) {
      // Business conflict from backend (DUPLICATE_TRANSFER, INSUFFICIENT_BALANCE, ACCOUNT_NOT_ACTIVE …)
      // Enrich the error so callers can inspect the specific code returned in the ErrorResponse.
      const serverMsg = error.response?.data?.message || 'Request conflict. Please check and retry.';
      const errorCode = error.response?.data?.error || 'CONFLICT';
      error.isConflict = true;
      error.conflictMessage = `[${errorCode}] ${serverMsg}`;
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;
