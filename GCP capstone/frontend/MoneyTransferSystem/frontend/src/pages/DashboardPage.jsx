import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import AccountService from "../services/AccountService";
import LoadingSpinner from "../components/common/LoadingSpinner";
import Navbar from "../components/common/Navbar";
import styles from "./DashboardPage.module.css";

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  // Backend account response: { id, holderName, balance, status, lastUpdated }
  const [account, setAccount] = useState(null);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);

  const accountId = user?.accountId || 1;

  useEffect(() => {
    let cancelled = false;
    AccountService.getAccount(accountId)
      .then((data) => { if (!cancelled) setAccount(data); })
      .catch((err) => {
        if (!cancelled) setFetchError(err.response?.data?.message || "Failed to load account data.");
      })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [accountId]);

  const greeting = () => {
    const h = new Date().getHours();
    if (h < 12) return "Good Morning";
    if (h < 17) return "Good Afternoon";
    return "Good Evening";
  };

  const balance = account?.balance ?? 0;
  const fmtBalance = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
  }).format(balance);

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  return (
    <>
      <Navbar />
      <main className={styles.page}>
        {loading && <LoadingSpinner message="Loading your account..." />}

        {fetchError && (
          <div className={styles.errorBanner} role="alert">⚠️ {fetchError}</div>
        )}

        {!loading && !fetchError && account && (
          <>
            {/* Welcome banner */}
            <div className={styles.welcomeBanner}>
              <div>
                <p className={styles.greetingText}>{greeting()},</p>
                <h1 className={styles.holderName}>{account.holderName} 👋</h1>
              </div>
              <p className={styles.dateText}>
                {new Date().toLocaleDateString("en-US", {
                  weekday: "long", year: "numeric", month: "long", day: "numeric",
                })}
              </p>
            </div>

            {/* Balance Card */}
            <div className={styles.balanceCard}>
              <p className={styles.balanceLabel}>Available Balance</p>
              <h2 className={styles.balanceAmount}>{fmtBalance}</h2>
              <p className={styles.accountMeta}>
                Account ID: {account.id}
                <span className={styles.acctTypeBadge}>{account.status}</span>
              </p>
            </div>

            {/* Quick action buttons */}
            <div className={styles.actionGrid}>
              <button className={styles.actionBtn} onClick={() => navigate("/transfer")}>
                <span className={styles.actionIcon}>💸</span>
                <span>Transfer</span>
              </button>
              <button className={styles.actionBtn} onClick={() => navigate("/transactions")}>
                <span className={styles.actionIcon}>📋</span>
                <span>History</span>
              </button>
              <button className={styles.actionBtn} onClick={() => navigate("/profile")}>
                <span className={styles.actionIcon}>👤</span>
                <span>Profile</span>
              </button>
            </div>

            {/* Account status row */}
            <div className={styles.statsRow}>
              <div className={styles.statItem}>
                <span className={styles.statLabel}>Account Holder</span>
                <span className={styles.statValue}>{account.holderName}</span>
              </div>
              <div className={styles.statDivider} />
              <div className={styles.statItem}>
                <span className={styles.statLabel}>Status</span>
                <span className={styles.statValue}>{account.status}</span>
              </div>
              <div className={styles.statDivider} />
              <div className={styles.statItem}>
                <span className={styles.statLabel}>Last Updated</span>
                <span className={styles.statValue}>
                  {account.lastUpdated
                    ? new Date(account.lastUpdated).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })
                    : "—"}
                </span>
              </div>
            </div>
          </>
        )}
      </main>
    </>
  );
}

