import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { v4 as uuidv4 } from 'uuid';
import { useNavigate } from 'react-router-dom';
import TransferService from '../../services/TransferService';
import { useAuth } from '../../context/AuthContext';
import styles from './TransferForm.module.css';

const TRANSFER_STEPS = ['Details', 'Review', 'Confirmation'];

/**
 * TransferForm — aligned to backend contract (report.md §5.2):
 *   POST /api/v1/transfers
 *   Body: { fromAccountId: number, toAccountId: number, amount: number, idempotencyKey: string }
 *
 * @param {{ account: { id, holderName, balance, status } | null }} props
 */
export default function TransferForm({ account = null }) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [step, setStep]               = useState(0);
  const [reviewData, setReviewData]   = useState(null);
  const [submitting, setSubmitting]   = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const [successData, setSuccessData] = useState(null);

  // Idempotency key is generated once when the user reaches the Review step (FR-05).
  const [idempotencyKey] = useState(() => uuidv4());

  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors },
  } = useForm({
    mode: 'onBlur',
    defaultValues: {
      fromAccountId: account?.id ?? user?.accountId ?? '',
      toAccountId: '',
      amount: '',
    },
  });

  const onDetailsSubmit = (data) => {
    setReviewData(data);
    setStep(1);
  };

  const handleConfirm = async () => {
    setSubmitting(true);
    setSubmitError(null);
    try {
      const result = await TransferService.transfer({
        fromAccountId: reviewData.fromAccountId,
        toAccountId:   reviewData.toAccountId,
        amount:        parseFloat(reviewData.amount),
        idempotencyKey,
      });
      setSuccessData(result);
      setStep(2);
    } catch (err) {
      if (err.isConflict) {
        setSubmitError(err.conflictMessage);
      } else {
        setSubmitError(err.message || err.response?.data?.message || 'Transfer failed. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleReset = () => {
    setStep(0);
    setReviewData(null);
    setSubmitError(null);
    setSuccessData(null);
  };

  const fmt = (n) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(n);

  return (
    <div className={styles.container}>
      <div className={styles.stepper} aria-label="Transfer steps">
        {TRANSFER_STEPS.map((label, i) => (
          <div key={label} className={styles.stepItem}>
            <div className={`${styles.stepCircle} ${i <= step ? styles.stepActive : ''}`}>
              {i < step ? 'v' : i + 1}
            </div>
            <span className={`${styles.stepLabel} ${i === step ? styles.stepLabelActive : ''}`}>
              {label}
            </span>
            {i < TRANSFER_STEPS.length - 1 && (
              <div className={`${styles.stepLine} ${i < step ? styles.stepLineDone : ''}`} />
            )}
          </div>
        ))}
      </div>

      {/* ── Step 0: Details ─────────────────────────────────────────────── */}
      {step === 0 && (
        <form className={styles.formBody} onSubmit={handleSubmit(onDetailsSubmit)} noValidate>
          <h2 className={styles.stepTitle}>Transfer Details</h2>

          {/* From Account — display card when account data is loaded */}
          {account && (
            <div className={styles.fromAccountDisplay}>
              <div className={styles.fromAvatar}>
                {(account.holderName || 'U')[0].toUpperCase()}
              </div>
              <div>
                <p className={styles.fromName}>From: {account.holderName}</p>
                <p className={styles.fromMeta}>
                  Account ID: {account.id} &nbsp;·&nbsp; Bal. {fmt(account.balance)}
                  &nbsp;·&nbsp; {account.status}
                </p>
              </div>
            </div>
          )}

          {/* From Account ID — numeric ID */}
          <div className={styles.field}>
            <label className={styles.label} htmlFor="fromAccountId">From Account ID</label>
            <input
              id="fromAccountId"
              type="number"
              inputMode="numeric"
              min="1"
              placeholder="Source account ID (e.g. 1)"
              className={`${styles.input} ${errors.fromAccountId ? styles.inputError : ''}`}
              {...register('fromAccountId', {
                required: 'Source account ID is required.',
                min: { value: 1, message: 'Account ID must be a positive number.' },
                validate: (v) => Number.isInteger(Number(v)) || 'Account ID must be a whole number.',
              })}
            />
            {errors.fromAccountId && (
              <span className={styles.error}>{errors.fromAccountId.message}</span>
            )}
          </div>

          {/* To Account ID — numeric ID of the destination */}
          <div className={styles.field}>
            <label className={styles.label} htmlFor="toAccountId">Recipient Account ID</label>
            <input
              id="toAccountId"
              type="number"
              inputMode="numeric"
              min="1"
              placeholder="Destination account ID (e.g. 2)"
              className={`${styles.input} ${errors.toAccountId ? styles.inputError : ''}`}
              {...register('toAccountId', {
                required: 'Recipient account ID is required.',
                min: { value: 1, message: 'Account ID must be a positive number.' },
                validate: (val) => {
                  if (!Number.isInteger(Number(val))) return 'Account ID must be a whole number.';
                  if (String(val) === String(getValues('fromAccountId'))) {
                    return 'Destination must differ from source account.';
                  }
                  return true;
                },
              })}
            />
            {errors.toAccountId && (
              <span className={styles.error}>{errors.toAccountId.message}</span>
            )}
          </div>

          {/* Amount — backend minimum 0.01, max 15.4 digits */}
          <div className={styles.field}>
            <label className={styles.label} htmlFor="amount">Amount (USD)</label>
            <div className={styles.amountWrapper}>
              <span className={styles.currency}>$</span>
              <input
                id="amount"
                type="number"
                min="0.01"
                step="0.01"
                inputMode="decimal"
                placeholder="0.00"
                className={`${styles.input} ${styles.amountInput} ${errors.amount ? styles.inputError : ''}`}
                {...register('amount', {
                  required: 'Amount is required.',
                  min: { value: 0.01, message: 'Minimum transfer amount is $0.01.' },
                  validate: (val) =>
                    /^\d+(\.\d{1,4})?$/.test(String(val)) || 'Max 4 decimal places allowed.',
                })}
              />
            </div>
            {errors.amount && <span className={styles.error}>{errors.amount.message}</span>}
          </div>

          <button type="submit" className={styles.primaryBtn}>
            Review Transfer
          </button>
        </form>
      )}

      {/* ── Step 1: Review & Confirm ──────────────────────────────────────── */}
      {step === 1 && reviewData && (
        <div className={styles.formBody}>
          <h2 className={styles.stepTitle}>Review and Confirm</h2>
          <div className={styles.reviewCard}>
            <ReviewRow label="From Account ID" value={reviewData.fromAccountId} />
            <ReviewRow label="To Account ID"   value={reviewData.toAccountId} />
            <ReviewRow label="Amount"          value={fmt(reviewData.amount)} highlight />
            <ReviewRow
              label="Date"
              value={new Date().toLocaleDateString('en-US', {
                weekday: 'short', year: 'numeric', month: 'short', day: 'numeric',
              })}
            />
          </div>

          <p className={styles.idempotencyNote}>
            A unique idempotency key is attached to this transfer to prevent duplicates (FR-05).
          </p>

          {submitError && (
            <div className={styles.errorBanner} role="alert">{submitError}</div>
          )}

          <div className={styles.btnRow}>
            <button
              className={styles.secondaryBtn}
              onClick={() => { setSubmitError(null); setStep(0); }}
              disabled={submitting}
            >
              Back
            </button>
            <button className={styles.primaryBtn} onClick={handleConfirm} disabled={submitting}>
              {submitting ? 'Processing...' : 'Confirm Transfer'}
            </button>
          </div>
        </div>
      )}

      {/* ── Step 2: Success ───────────────────────────────────────────────── */}
      {step === 2 && (
        <div className={styles.successBody}>
          <div className={styles.successIcon}>✅</div>
          <h2 className={styles.successTitle}>Transfer Successful!</h2>
          <p className={styles.successSub}>{fmt(reviewData.amount)} sent successfully.</p>
          {successData?.transactionId && (
            <p className={styles.txnId}>
              Transaction ID: <strong>{successData.transactionId}</strong>
            </p>
          )}
          {successData?.status && (
            <p className={styles.txnId}>Status: <strong>{successData.status}</strong></p>
          )}
          <div className={styles.btnRow}>
            <button className={styles.secondaryBtn} onClick={handleReset}>New Transfer</button>
            <button className={styles.primaryBtn} onClick={() => navigate('/transactions')}>
              View History
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function ReviewRow({ label, value, highlight }) {
  return (
    <div className={styles.reviewRow}>
      <span className={styles.reviewLabel}>{label}</span>
      <span className={`${styles.reviewValue} ${highlight ? styles.reviewHighlight : ''}`}>
        {value}
      </span>
    </div>
  );
}
