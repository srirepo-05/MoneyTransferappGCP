import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { getAccountByIdApi } from '../api/accounts';
import TransferForm from '../components/transfer/TransferForm';
import LoadingSpinner from '../components/common/LoadingSpinner';
import Navbar from '../components/common/Navbar';
import styles from './TransferPage.module.css';

export default function TransferPage() {
  const { user } = useAuth();
  const [account, setAccount] = useState(null);
  const [loading, setLoading] = useState(true);

  const accountId = user?.accountId || 1;

  useEffect(() => {
    let cancelled = false;
    getAccountByIdApi(accountId)
      .then(({ data }) => {
        if (!cancelled) setAccount(data);
      })
      .catch(() => {
        // Non-critical: form falls back to manual entry
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, [accountId]);

  return (
    <>
      <Navbar />
      <main className={styles.page}>
        <h1 className={styles.pageTitle}>Fund Transfer</h1>
        <p className={styles.pageSubtitle}>
          Send money securely between accounts in three simple steps.
        </p>
        {loading ? (
          <LoadingSpinner message="Loading account…" />
        ) : (
          <TransferForm account={account} />
        )}
      </main>
    </>
  );
}

