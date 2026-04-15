package com.example.moneytransfer.domain.entity;

import com.example.moneytransfer.domain.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity that records every attempted money transfer as an audit log.
 * Stores the idempotency key to guarantee at-most-once processing semantics.
 */
@Entity
@Table(
        name = "transaction_logs",
        indexes = {
                @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class TransactionLog {

    /** UUID stored as VARCHAR(36) — matches schema id VARCHAR(36) PRIMARY KEY. */
    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "from_account", nullable = false)
    private Long fromAccountId;

    @Column(name = "to_account", nullable = false)
    private Long toAccountId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    /** Human-readable reason recorded when {@code status == FAILED}. */
    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    /**
     * Client-generated UUID used for idempotency.
     * A duplicate submission with the same key is rejected with {@code DuplicateTransferException}.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdOn = LocalDateTime.now();
    }
}
