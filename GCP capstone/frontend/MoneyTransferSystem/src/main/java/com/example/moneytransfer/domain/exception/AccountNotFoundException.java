package com.example.moneytransfer.domain.exception;

/**
 * Thrown when a requested account ID does not exist in the system.
 */
public class AccountNotFoundException extends MoneyTransferException {

    public AccountNotFoundException(Long accountId) {
        super("Account not found with ID: " + accountId);
    }

    public AccountNotFoundException(String message) {
        super(message);
    }
}
