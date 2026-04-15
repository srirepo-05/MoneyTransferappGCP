package com.example.moneytransfer.domain.exception;

/**
 * Thrown when a transfer request is submitted with an idempotency key that has already been processed.
 */
public class DuplicateTransferException extends MoneyTransferException {

    public DuplicateTransferException(String idempotencyKey) {
        super("A transfer with idempotency key '" + idempotencyKey + "' has already been processed.");
    }

    public DuplicateTransferException(String message, boolean raw) {
        super(message);
    }
}
