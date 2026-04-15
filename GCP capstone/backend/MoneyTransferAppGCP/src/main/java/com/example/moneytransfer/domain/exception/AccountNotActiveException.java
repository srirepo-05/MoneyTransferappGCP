package com.example.moneytransfer.domain.exception;

/**
 * Thrown when an operation is attempted on a LOCKED or CLOSED account.
 */
public class AccountNotActiveException extends MoneyTransferException {

    public AccountNotActiveException(Long accountId) {
        super("Account is not active. Account ID: " + accountId);
    }

    public AccountNotActiveException(String message) {
        super(message);
    }
}
