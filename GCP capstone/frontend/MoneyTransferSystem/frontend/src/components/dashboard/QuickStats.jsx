import styles from './QuickStats.module.css';

export default function QuickStats({ totalIncome = 0, totalExpenses = 0, transactionsCount = 0 }) {
  const fmt = (n) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(n);

  const stats = [
    { label: 'Monthly Income', value: fmt(totalIncome), icon: '📈', color: 'green' },
    { label: 'Monthly Expenses', value: fmt(totalExpenses), icon: '📉', color: 'red' },
    { label: 'Transactions', value: transactionsCount, icon: '🔁', color: 'blue' },
  ];

  return (
    <div className={styles.grid}>
      {stats.map(({ label, value, icon, color }) => (
        <div key={label} className={`${styles.stat} ${styles[color]}`}>
          <span className={styles.icon}>{icon}</span>
          <div className={styles.details}>
            <span className={styles.label}>{label}</span>
            <span className={styles.value}>{value}</span>
          </div>
        </div>
      ))}
    </div>
  );
}
