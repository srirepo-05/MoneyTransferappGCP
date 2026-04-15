import axiosInstance from './axiosInstance';

/**
 * Authentication helpers — src/api/auth.js
 *
 * The Spring Boot backend uses stateless HTTP Basic Auth on all /api/v1/** endpoints.
 * There is NO dedicated /auth/login, /auth/logout or /auth/me endpoint.
 *
 * Login flow (FR-08):
 *   1. Encode "username:password" as Base64.
 *   2. Perform a probe request to a real protected endpoint to validate the credentials.
 *   3. If the probe returns 200, store the token; if 401, reject.
 */

/**
 * Validates Basic Auth credentials by probing GET /accounts/1.
 * The token must be passed explicitly because it is not yet in localStorage.
 * @returns {Promise} resolves with the account data if credentials are valid
 */
export const validateCredentialsApi = (token) =>
  axiosInstance.get('/accounts/1', {
    headers: { Authorization: `Basic ${token}` },
  });
