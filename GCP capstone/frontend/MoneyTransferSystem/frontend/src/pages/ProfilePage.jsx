import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import AccountService from "../services/AccountService";
import Navbar from "../components/common/Navbar";
import LoadingSpinner from "../components/common/LoadingSpinner";
import styles from "./ProfilePage.module.css";

export default function ProfilePage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  const accountId = user?.accountId || 1;

  useEffect(() => {
    let cancelled = false;
    AccountService.getAccount(accountId)
      .then((data) => { if (!cancelled) setSummary(data); })
      .catch(() => {})
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [accountId]);

  // Backend account shape: { id, holderName, balance, status, lastUpdated }
  const primaryAccount = summary;
  // Use holderName from account, fall back to username
  const displayName = primaryAccount?.holderName || user?.fullName || user?.username || "User";

  const initials = displayName
    .split(" ")
    .filter((w) => /^[a-zA-Z]/.test(w))
    .map((w) => w[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  const fmtBalance = primaryAccount?.balance != null
    ? new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(primaryAccount.balance)
    : "—";

  const memberSince = primaryAccount?.lastUpdated
    ? new Date(primaryAccount.lastUpdated).toLocaleDateString("en-US", { month: "long", year: "numeric" })
    : "N/A";

  return (
    <>
      <Navbar />
      <main className={styles.page}>
        <button className={styles.backBtn} onClick={() => navigate("/dashboard")}>
          ← Back to Dashboard
        </button>

        {loading ? (
          <LoadingSpinner message="Loading profile..." />
        ) : (
          <>
            {/* Avatar + name card */}
            <div className={styles.heroCard}>
              <div className={styles.avatarRing}>
                <div className={styles.avatar}>{initials}</div>
              </div>
              <h1 className={styles.fullName}>{displayName}</h1>
              <p className={styles.username}>@{user?.username}</p>
              <span className={styles.memberBadge}>
                🎖️ Member since {memberSince}
              </span>
            </div>

            {/* Info cards */}
            <div className={styles.section}>
              <h2 className={styles.sectionTitle}>Personal Information</h2>
              <div className={styles.infoGrid}>
                <div className={styles.infoItem}>
                  <span className={styles.infoIcon}>👤</span>
                  <div>
                    <p className={styles.infoLabel}>Full Name</p>
                    <p className={styles.infoValue}>{displayName}</p>
                  </div>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoIcon}>🔑</span>
                  <div>
                    <p className={styles.infoLabel}>Username</p>
                    <p className={styles.infoValue}>@{user?.username || "—"}</p>
                  </div>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoIcon}>📧</span>
                  <div>
                    <p className={styles.infoLabel}>Email</p>
                    <p className={styles.infoValue}>{user?.email || "—"}</p>
                  </div>
                </div>
                <div className={styles.infoItem}>
                  <span className={styles.infoIcon}>📱</span>
                  <div>
                    <p className={styles.infoLabel}>Phone</p>
                    <p className={styles.infoValue}>{user?.phone || "—"}</p>
                  </div>
                </div>
              </div>
            </div>

            {/* Account details */}
            {primaryAccount && (
              <div className={styles.section}>
                <h2 className={styles.sectionTitle}>Account Details</h2>
                <div className={styles.accountCard}>
                  <div className={styles.accountCardBg} />
                  <div className={styles.accountCardRow}>
                    <span className={styles.acctLabel}>Account ID</span>
                    <span className={styles.acctValue}>{primaryAccount.id}</span>
                  </div>
                  <div className={styles.accountCardRow}>
                    <span className={styles.acctLabel}>Holder</span>
                    <span className={styles.acctBadge}>{primaryAccount.holderName}</span>
                  </div>
                  <div className={styles.accountCardRow}>
                    <span className={styles.acctLabel}>Available Balance</span>
                    <span className={styles.acctBalance}>{fmtBalance}</span>
                  </div>
                  <div className={styles.accountCardRow}>
                    <span className={styles.acctLabel}>Status</span>
                    <span className={styles.acctStatusBadge}>
                      {primaryAccount.status === "ACTIVE" ? "✅" : "🔒"} {primaryAccount.status}
                    </span>
                  </div>
                </div>
              </div>
            )}

            {/* Quick actions */}
            <div className={styles.section}>
              <h2 className={styles.sectionTitle}>Quick Actions</h2>
              <div className={styles.actionList}>
                <button className={styles.actionItem} onClick={() => navigate("/transfer")}>
                  <span className={styles.actionItemIcon}>💸</span>
                  <span className={styles.actionItemLabel}>Make a Transfer</span>
                  <span className={styles.actionItemArrow}>›</span>
                </button>
                <button className={styles.actionItem} onClick={() => navigate("/transactions")}>
                  <span className={styles.actionItemIcon}>📋</span>
                  <span className={styles.actionItemLabel}>Transaction History</span>
                  <span className={styles.actionItemArrow}>›</span>
                </button>
              </div>
            </div>
          </>
        )}
      </main>
    </>
  );
}
