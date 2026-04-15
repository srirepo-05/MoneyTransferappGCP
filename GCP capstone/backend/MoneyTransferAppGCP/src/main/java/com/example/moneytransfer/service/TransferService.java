package com.example.moneytransfer.service;

import com.example.moneytransfer.domain.dto.TransferRequest;
import com.example.moneytransfer.domain.dto.TransferResponse;
import com.example.moneytransfer.domain.entity.Account;
import com.example.moneytransfer.domain.entity.TransactionLog;
import com.example.moneytransfer.domain.enums.TransactionStatus;
import com.example.moneytransfer.domain.exception.DuplicateTransferException;
import com.example.moneytransfer.repository.AccountRepository;
import com.example.moneytransfer.repository.TransactionLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final AccountService accountService;
    // Optional — only present when bigquery.enabled=true
    private final Optional<BigQueryService> bigQueryService;

    public TransferService(AccountRepository accountRepository,
                           TransactionLogRepository transactionLogRepository,
                           AccountService accountService,
                           Optional<BigQueryService> bigQueryService) {
        this.accountRepository        = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.accountService           = accountService;
        this.bigQueryService          = bigQueryService;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Executes a fund transfer atomically.
     *
     * <p>Flow:
     * <ol>
     *   <li>Idempotency check — reject duplicate keys immediately.
     *   <li>Lock both accounts in a deterministic order to prevent deadlocks.
     *   <li>Validate both accounts are ACTIVE.
     *   <li>Debit source, credit destination.
     *   <li>Persist a SUCCESS {@link TransactionLog}.
     * </ol>
     *
     * <p>Any domain exception causes the transaction to roll back and a FAILED
     * log is written in a separate, independent transaction.
     */
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        // 1. Idempotency guard
        checkIdempotency(request.getIdempotencyKey());

        // 2. Lock accounts in a fixed order (lower id first) to prevent deadlocks
        Account from;
        Account to;
        if (request.getFromAccountId() < request.getToAccountId()) {
            from = lockAccount(request.getFromAccountId());
            to   = lockAccount(request.getToAccountId());
        } else {
            to   = lockAccount(request.getToAccountId());
            from = lockAccount(request.getFromAccountId());
        }

        // 3. Execute business logic — domain methods throw on rule violations
        from.debit(request.getAmount());
        to.credit(request.getAmount());

        accountRepository.save(from);
        accountRepository.save(to);

        // 4. Persist success log
        TransactionLog txLog = buildLog(request, TransactionStatus.SUCCESS, null);
        transactionLogRepository.save(txLog);

        log.info("Transfer SUCCESS | key={} from={} to={} amount={}",
                request.getIdempotencyKey(),
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount());

        // 5. Stream to BigQuery asynchronously (fire-and-forget, never blocks response)
        bigQueryService.ifPresent(bq -> {
            bq.streamTransaction(txLog);
            bq.streamAccount(from);
            bq.streamAccount(to);
        });

        return toResponse(txLog);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void checkIdempotency(String key) {
        if (transactionLogRepository.existsByIdempotencyKey(key)) {
            throw new DuplicateTransferException(key);
        }
    }

    private Account lockAccount(Long id) {
        return accountRepository.findByIdWithLock(id)
                .orElseThrow(() -> new com.example.moneytransfer.domain.exception.AccountNotFoundException(id));
    }

    private TransactionLog buildLog(TransferRequest req,
                                    TransactionStatus status,
                                    String failureReason) {
        return TransactionLog.builder()
                .fromAccountId(req.getFromAccountId())
                .toAccountId(req.getToAccountId())
                .amount(req.getAmount())
                .status(status)
                .failureReason(failureReason)
                .idempotencyKey(req.getIdempotencyKey())
                .build();
    }

    private TransferResponse toResponse(TransactionLog log) {
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
