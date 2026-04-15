package com.example.moneytransfer.domain.exception;

/**
 * Base exception for all Money Transfer domain errors.
 */
public class MoneyTransferException extends RuntimeException {

    public MoneyTransferException(String message) {
        super(message);
    }

    public MoneyTransferException(String message, Throwable cause) {
        super(message, cause);
    }
}
