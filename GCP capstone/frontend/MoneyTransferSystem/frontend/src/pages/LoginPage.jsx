import { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { validators } from '../utils/validators';
import styles from './LoginPage.module.css';

const INITIAL_FORM = { username: '', password: '' };
const INITIAL_ERRORS = { username: '', password: '' };

export default function LoginPage() {
  const { login, isAuthenticated, loading, error, clearError } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const redirectTo = location.state?.from?.pathname || '/dashboard';

  const [form, setForm] = useState(INITIAL_FORM);
  const [fieldErrors, setFieldErrors] = useState(INITIAL_ERRORS);
  const [showPassword, setShowPassword] = useState(false);

  // Redirect if already logged in
  useEffect(() => {
    if (isAuthenticated) navigate(redirectTo, { replace: true });
  }, [isAuthenticated, navigate, redirectTo]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    // Clear field-level error on edit
    setFieldErrors((prev) => ({ ...prev, [name]: '' }));
    clearError();
  };

  const validate = () => {
    const errs = {};
    const usernameErr = validators.compose(
      validators.required,
      validators.minLength(3)
    )(form.username);
    const passwordErr = validators.compose(
      validators.required,
      validators.minLength(5)
    )(form.password);
    if (usernameErr) errs.username = usernameErr;
    if (passwordErr) errs.password = passwordErr;
    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) {
      setFieldErrors(errs);
      return;
    }
    const result = await login({ username: form.username, password: form.password });
    if (result.success) {
      navigate(redirectTo, { replace: true });
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        {/* Header */}
        <div className={styles.header}>
          <span className={styles.logo}>🏦</span>
          <h1 className={styles.title}>BankApp</h1>
          <p className={styles.subtitle}>Sign in to your account</p>
        </div>

        {/* Global API error */}
        {error && (
          <div className={styles.alert} role="alert">
            <span>⚠️</span> {error}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate className={styles.form}>
          {/* Username */}
          <div className={styles.field}>
            <label htmlFor="username" className={styles.label}>Username</label>
            <input
              id="username"
              name="username"
              type="text"
              autoComplete="username"
              value={form.username}
              onChange={handleChange}
              className={`${styles.input} ${fieldErrors.username ? styles.inputError : ''}`}
              placeholder="Enter your username"
              disabled={loading}
            />
            {fieldErrors.username && (
              <span className={styles.errorMsg}>{fieldErrors.username}</span>
            )}
          </div>

          {/* Password */}
          <div className={styles.field}>
            <label htmlFor="password" className={styles.label}>Password</label>
            <div className={styles.passwordWrapper}>
              <input
                id="password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                value={form.password}
                onChange={handleChange}
                className={`${styles.input} ${fieldErrors.password ? styles.inputError : ''}`}
                placeholder="Enter your password"
                disabled={loading}
              />
              <button
                type="button"
                className={styles.togglePwd}
                onClick={() => setShowPassword((v) => !v)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? '🙈' : '👁️'}
              </button>
            </div>
            {fieldErrors.password && (
              <span className={styles.errorMsg}>{fieldErrors.password}</span>
            )}
          </div>

          <button
            type="submit"
            className={styles.submitBtn}
            disabled={loading}
          >
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>

        <p className={styles.demo}>
          🔑 <strong>Demo mode:</strong> any username + password <code>password</code>
        </p>
      </div>
    </div>
  );
}
