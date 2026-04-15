package com.example.moneytransfer.service;

import com.example.moneytransfer.domain.dto.AccountResponse;
import com.example.moneytransfer.domain.dto.TransferResponse;
import com.example.moneytransfer.domain.entity.Account;
import com.example.moneytransfer.domain.entity.TransactionLog;
import com.example.moneytransfer.domain.enums.AccountStatus;
import com.example.moneytransfer.domain.exception.AccountNotFoundException;
import com.example.moneytransfer.repository.AccountRepository;
import com.example.moneytransfer.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long id) {
        Account account = findOrThrow(id);
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long id) {
        return findOrThrow(id).getBalance();
    }

    @Transactional(readOnly = true)
    public Page<TransferResponse> getTransactions(Long id, Pageable pageable) {
        // ensure account exists
        findOrThrow(id);
        return transactionLogRepository
                .findByFromAccountIdOrToAccountId(id, id, pageable)
                .map(this::toTransferResponse);
    }

    // -----------------------------------------------------------------------
    // Admin helpers (used by seed / tests)
    // -----------------------------------------------------------------------

    @Transactional
    public AccountResponse createAccount(String holderName, BigDecimal initialBalance) {
        Account account = Account.builder()
                .holderName(holderName)
                .balance(initialBalance)
                .status(AccountStatus.ACTIVE)
                .version(0)
                .build();
        return toResponse(accountRepository.save(account));
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    Account findOrThrow(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    private AccountResponse toResponse(Account a) {
        return AccountResponse.builder()
                .id(a.getId())
                .holderName(a.getHolderName())
                .balance(a.getBalance())
                .status(a.getStatus())
                .lastUpdated(a.getLastUpdated())
                .build();
    }

    private TransferResponse toTransferResponse(TransactionLog log) {
        return TransferResponse.builder()
                .transactionId(log.getId())
                .fromAccountId(log.getFromAccountId())
                .toAccountId(log.getToAccountId())
                .amount(log.getAmount())
                .status(log.getStatus())
                .failureReason(log.getFailureReason())
                .idempotencyKey(log.getIdempotencyKey())
                .createdOn(log.getCreatedOn())
                .build();
    }
}
