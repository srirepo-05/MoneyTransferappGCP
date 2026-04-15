package com.example.moneytransfer.domain.exception;

import java.math.BigDecimal;

/**
 * Thrown when an account's balance is insufficient for the requested transfer amount.
 */
public class InsufficientBalanceException extends MoneyTransferException {

    public InsufficientBalanceException(Long accountId, BigDecimal balance, BigDecimal requested) {
        super(String.format(
                "Insufficient balance on account %d. Available: %.2f, Requested: %.2f",
                accountId, balance, requested));
    }

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
