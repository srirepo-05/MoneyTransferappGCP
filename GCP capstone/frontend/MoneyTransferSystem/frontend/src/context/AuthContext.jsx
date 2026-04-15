import { createContext, useCallback, useContext, useMemo, useState } from "react";
import AuthService from "../services/AuthService";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => AuthService.getUser());
  const [token, setToken] = useState(() => AuthService.getToken());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const isAuthenticated = Boolean(token);

  const login = useCallback(async ({ username, password }) => {
    setLoading(true);
    setError(null);
    const result = await AuthService.login(username, password);
    if (result.success) {
      setToken(AuthService.getToken());
      setUser(result.user);
    } else {
      setError(result.message);
    }
    setLoading(false);
    return result;
  }, []);

  const logout = useCallback(async () => {
    await AuthService.logout();
    setToken(null);
    setUser(null);
  }, []);

  const clearError = useCallback(() => setError(null), []);

  const value = useMemo(
    () => ({ user, token, isAuthenticated, loading, error, login, logout, clearError }),
    [user, token, isAuthenticated, loading, error, login, logout, clearError]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}
