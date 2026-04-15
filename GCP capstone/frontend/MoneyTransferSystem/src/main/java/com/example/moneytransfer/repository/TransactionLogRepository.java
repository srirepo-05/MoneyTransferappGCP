package com.example.moneytransfer.repository;

import com.example.moneytransfer.domain.entity.TransactionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {

    /** Used by idempotency check — returns existing log for a given key if present. */
    Optional<TransactionLog> findByIdempotencyKey(String idempotencyKey);

    /** All transactions where the given account is the sender. */
    Page<TransactionLog> findByFromAccountId(Long fromAccountId, Pageable pageable);

    /** All transactions where the given account is the receiver. */
    Page<TransactionLog> findByToAccountId(Long toAccountId, Pageable pageable);

    /** All transactions (sent or received) for an account, filtered by status. */
    Page<TransactionLog> findByFromAccountIdOrToAccountId(
            Long fromAccountId, Long toAccountId, Pageable pageable);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
