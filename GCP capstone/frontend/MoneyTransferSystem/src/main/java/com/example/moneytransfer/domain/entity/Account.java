package com.example.moneytransfer.domain.entity;

import com.example.moneytransfer.domain.enums.AccountStatus;
import com.example.moneytransfer.domain.exception.AccountNotActiveException;
import com.example.moneytransfer.domain.exception.InsufficientBalanceException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity representing a bank account.
 * Encapsulates balance mutation logic (debit / credit) with business-rule enforcement.
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "version")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holder_name", nullable = false, length = 255)
    private String holderName;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    /**
     * Optimistic-locking version field — prevents concurrent balance corruption.
     */
    @Version
    @Column(nullable = false)
    private Integer version;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // -----------------------------------------------------------------------
    // Business methods
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} when the account is in {@link AccountStatus#ACTIVE} state.
     */
    public boolean isActive() {
        return AccountStatus.ACTIVE.equals(this.status);
    }

    /**
     * Deducts {@code amount} from the account balance.
     *
     * @param amount positive value to deduct
     * @throws AccountNotActiveException    if the account is not ACTIVE
     * @throws InsufficientBalanceException if the balance would go below zero
     * @throws IllegalArgumentException     if amount is null or non-positive
     */
    public void debit(BigDecimal amount) {
        validateAmount(amount);
        if (!isActive()) {
            throw new AccountNotActiveException(this.id);
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(this.id, this.balance, amount);
        }
        this.balance = this.balance.subtract(amount);
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Adds {@code amount} to the account balance.
     *
     * @param amount positive value to add
     * @throws AccountNotActiveException if the account is not ACTIVE
     * @throws IllegalArgumentException  if amount is null or non-positive
     */
    public void credit(BigDecimal amount) {
        validateAmount(amount);
        if (!isActive()) {
            throw new AccountNotActiveException(this.id);
        }
        this.balance = this.balance.add(amount);
        this.lastUpdated = LocalDateTime.now();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be a positive value, got: " + amount);
        }
    }
}
