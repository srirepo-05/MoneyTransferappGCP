import styles from './TransactionTable.module.css';

/**
 * TransactionTable — renders a list of TransferResponse objects.
 *
 * Backend TransferResponse shape (report.md §5.2):
 *   { transactionId, fromAccountId, toAccountId, amount, status,
 *     failureReason, idempotencyKey, createdOn }
 *
 * @param {{ transactions: TransferResponse[], accountId: number }} props
 *   accountId  — the viewer’s own account ID, used to determine debit vs credit.
 */
export default function TransactionTable({ transactions, accountId }) {
  if (!transactions || transactions.length === 0) {
    return (
      <div className={styles.empty}>
        <span>📂</span>
        <p>No transactions found for the selected filters.</p>
      </div>
    );
  }

  return (
    <div className={styles.tableWrapper}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>Date</th>
            <th>Direction</th>
            <th>Counterpart</th>
            <th className={styles.amountCol}>Amount</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((txn) => {
            const isDebit  = String(txn.fromAccountId) === String(accountId);
            const dir      = isDebit ? 'DEBIT' : 'CREDIT';
            const dirLabel = isDebit ? 'Sent' : 'Received';
            const dirIcon  = isDebit ? '↑' : '↓';
            const dirCss   = isDebit ? 'debit' : 'credit';
            const counterpart = isDebit ? txn.toAccountId : txn.fromAccountId;

            const date    = new Date(txn.createdOn);
            const fmtDate = date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
            const fmtTime = date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
            const fmtAmount = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })
              .format(Math.abs(txn.amount));

            return (
              <tr key={txn.transactionId} className={styles.row}>
                <td className={styles.dateCell}>
                  <span>{fmtDate}</span>
                  <span className={styles.time}>{fmtTime}</span>
                </td>
                <td>
                  <span className={`${styles.typeBadge} ${styles[dirCss]}`}>
                    {dirIcon} {dirLabel}
                  </span>
                </td>
                <td className={styles.acctCell}>
                  Account #{counterpart}
                </td>
                <td className={`${styles.amountCol} ${styles[`${dirCss}Amt`]}`}>
                  {dir === 'CREDIT' ? '+' : '-'}{fmtAmount}
                </td>
                <td>
                  <span className={`${styles.statusBadge} ${styles[txn.status?.toLowerCase()]}`}>
                    {txn.status === 'SUCCESS' ? 'Success' : 'Failed'}
                  </span>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
