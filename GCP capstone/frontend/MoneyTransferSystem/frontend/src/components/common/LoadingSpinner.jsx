import styles from './LoadingSpinner.module.css';

export default function LoadingSpinner({ size = 'md', message = '' }) {
  return (
    <div className={styles.wrapper}>
      <div className={`${styles.spinner} ${styles[size]}`} role="status" aria-label="Loading" />
      {message && <p className={styles.message}>{message}</p>}
    </div>
  );
}
