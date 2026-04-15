import styles from './BalanceCard.module.css';

/**
 * BalanceCard — displays a single account card.
 *
 * Backend account shape (report.md §5.1):
 *   { id, holderName, balance, status, lastUpdated }
 */
export default function BalanceCard({ account }) {
  const { id, holderName, balance, status } = account;

  const formatted = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
  }).format(balance);

  return (
    <div className={`${styles.card} ${styles[status?.toLowerCase()] || ''}`}>
      <div className={styles.topRow}>
        <span className={styles.acctType}>{holderName}</span>
        <span className={styles.acctNumber}>ID: {id}</span>
      </div>
      <div className={styles.balanceRow}>
        <span className={styles.balanceLabel}>Available Balance</span>
        <span className={styles.balanceAmount}>{formatted}</span>
      </div>
      <div className={styles.topRow}>
        <span className={styles.acctType}>Status</span>
        <span className={styles.acctNumber}>{status}</span>
      </div>
    </div>
  );
}
