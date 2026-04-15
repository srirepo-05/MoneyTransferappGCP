import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import AccountService from "../services/AccountService";
import Navbar from "../components/common/Navbar";
import LoadingSpinner from "../components/common/LoadingSpinner";
import styles from "./TransactionHistoryPage.module.css";

const PAGE_SIZE = 10;
// Direction filter is resolved client-side from fromAccountId / toAccountId.
const TABS = [
  { key: "", label: "All" },
  { key: "DEBIT", label: "Sent" },
  { key: "CREDIT", label: "Received" },
];

export default function TransactionHistoryPage() {
  const { user } = useAuth();
  // Backend transactions: { transactionId, fromAccountId, toAccountId, amount,
  //                         status, failureReason, idempotencyKey, createdOn }
  const [allContent, setAllContent]   = useState([]);   // page content from backend
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages]   = useState(1);
  const [page, setPage]               = useState(0);     // 0-based (backend convention)
  const [activeTab, setActiveTab]     = useState("");
  const [loading, setLoading]         = useState(false);
  const [fetchError, setFetchError]   = useState(null);

  const accountId = user?.accountId || 1;

  const fetchTransactions = useCallback(async (currentPage) => {
    setLoading(true);
    setFetchError(null);
    try {
      const result = await AccountService.getTransactions(accountId, {
        page: currentPage,
        size: PAGE_SIZE,
      });
      // Spring Page shape: content, totalElements, totalPages, number
      setAllContent(result.content || []);
      setTotalElements(result.totalElements ?? 0);
      setTotalPages(result.totalPages ?? 1);
    } catch (err) {
      setFetchError(err.response?.data?.message || "Failed to load transactions.");
    } finally {
      setLoading(false);
    }
  }, [accountId]);

  useEffect(() => {
    fetchTransactions(page);
  }, [fetchTransactions, page]);

  const handleTabChange = (key) => {
    setActiveTab(key);
    setPage(0);
  };

  /**
   * Determines direction from the perspective of the logged-in account.
   * fromAccountId === accountId → DEBIT (sent), otherwise CREDIT (received).
   */
  const getDirection = (txn) =>
    String(txn.fromAccountId) === String(accountId) ? "DEBIT" : "CREDIT";

  // Client-side tab filter (direction not a backend query param)
  const transactions = activeTab
    ? allContent.filter((t) => getDirection(t) === activeTab)
    : allContent;

  const fmt = (n) =>
    new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(Math.abs(n));

  return (
    <>
      <Navbar />
      <main className={styles.page}>
        <h1 className={styles.pageTitle}>Transaction History</h1>

        {/* Tabs: All / Sent / Received */}
        <div className={styles.tabs} role="tablist">
          {TABS.map((t) => (
            <button
              key={t.key}
              role="tab"
              aria-selected={activeTab === t.key}
              className={`${styles.tab} ${activeTab === t.key ? styles.tabActive : ""}`}
              onClick={() => handleTabChange(t.key)}
            >
              {t.label}
            </button>
          ))}
        </div>

        {/* Count badge */}
        <div className={styles.searchRow}>
          <span className={styles.countBadge}>{totalElements} transactions total</span>
        </div>

        {fetchError && (
          <div className={styles.errorBanner} role="alert">⚠️ {fetchError}</div>
        )}

        {loading ? (
          <LoadingSpinner message="Loading transactions..." />
        ) : transactions.length === 0 ? (
          <div className={styles.empty}>
            <span>📂</span>
            <p>No transactions found.</p>
          </div>
        ) : (
          <div className={styles.txnList}>
            {transactions.map((txn) => {
              const isCredit = getDirection(txn) === "CREDIT";
              const date = new Date(txn.createdOn);
              const counterpart = isCredit ? txn.fromAccountId : txn.toAccountId;
              return (
                <div key={txn.transactionId} className={styles.txnRow}>
                  <div className={`${styles.txnAvatar} ${isCredit ? styles.avatarCredit : styles.avatarDebit}`}>
                    {isCredit ? "↓" : "↑"}
                  </div>
                  <div className={styles.txnInfo}>
                    <p className={styles.txnDesc}>
                      {isCredit ? `Received from Account #${counterpart}` : `Sent to Account #${counterpart}`}
                    </p>
                    <p className={styles.txnMeta}>
                      {date.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })}
                      {" · "}
                      {date.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" })}
                    </p>
                    {txn.failureReason && (
                      <p className={styles.txnMeta} style={{ color: "var(--color-danger, #dc3545)" }}>
                        {txn.failureReason}
                      </p>
                    )}
                  </div>
                  <div className={styles.txnRight}>
                    <p className={`${styles.txnAmount} ${isCredit ? styles.amtCredit : styles.amtDebit}`}>
                      {isCredit ? "+" : "-"}{fmt(txn.amount)}
                    </p>
                    <span className={`${styles.statusBadge} ${styles[(txn.status || "SUCCESS").toLowerCase()]}`}>
                      {txn.status === "SUCCESS" ? "Success" : "Failed"}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {!loading && totalPages > 1 && (
          <div className={styles.pagination}>
            <button
              className={styles.pageBtn}
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              ← Prev
            </button>
            <span className={styles.pageInfo}>Page {page + 1} of {totalPages}</span>
            <button
              className={styles.pageBtn}
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Next →
            </button>
          </div>
        )}
      </main>
    </>
  );
}

