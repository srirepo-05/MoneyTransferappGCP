/**
 * TransferService — transfer()
 *
 * Backend endpoint (report.md §5.2):
 *   POST /api/v1/transfers
 *   Request:  { fromAccountId: number, toAccountId: number, amount: number, idempotencyKey: string }
 *   Response: { transactionId, fromAccountId, toAccountId, amount, status, failureReason,
 *               idempotencyKey, createdOn }
 *
 * FR-05 : A new UUID idempotency key is generated here if not supplied by the caller.
 * 409 codes: DUPLICATE_TRANSFER | ACCOUNT_NOT_ACTIVE | INSUFFICIENT_BALANCE
 */
import { v4 as uuidv4 } from 'uuid';
import axiosInstance from '../api/axiosInstance';
import { addMockTransfer } from '../api/mockData';

const isNetworkError = (err) =>
  !err.response && (err.code === 'ERR_NETWORK' || err.code === 'ECONNREFUSED' || err.message === 'Network Error');

const TransferService = {
  /**
   * Initiates a fund transfer.
   *
   * @param {{ fromAccountId: number, toAccountId: number, amount: number, idempotencyKey?: string }} payload
   * @returns {Promise<TransferResponse>}
   */
  async transfer(payload) {
    const requestPayload = {
      fromAccountId: Number(payload.fromAccountId),
      toAccountId:   Number(payload.toAccountId),
      amount:        parseFloat(payload.amount),
      idempotencyKey: payload.idempotencyKey || uuidv4(),
    };

    try {
      const { data } = await axiosInstance.post('/transfers', requestPayload);
      return data;
    } catch (err) {
      if (err.response?.status === 409) {
        // Business conflict — surface backend error code and message to the caller.
        const errorCode = err.response.data?.error || 'CONFLICT';
        const message   = err.response.data?.message || 'Transfer could not be completed.';
        const conflict  = new Error(message);
        conflict.isConflict     = true;
        conflict.errorCode      = errorCode;
        conflict.conflictMessage = `${message}`;
        throw conflict;
      }
      if (err.response?.status === 400) {
        // Validation failure — surface field errors from ErrorResponse
        const fe = err.response.data?.fieldErrors;
        const message = fe?.length
          ? fe.map((f) => `${f.field}: ${f.message}`).join('; ')
          : err.response.data?.message || 'Invalid transfer request.';
        throw new Error(message);
      }
      if (isNetworkError(err)) {
        return addMockTransfer(requestPayload);
      }
      throw err;
    }
  },
};

export default TransferService;
