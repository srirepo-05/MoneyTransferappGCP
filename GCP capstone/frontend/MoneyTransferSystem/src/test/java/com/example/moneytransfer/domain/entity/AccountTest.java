package com.example.moneytransfer.domain.entity;

import com.example.moneytransfer.domain.enums.AccountStatus;
import com.example.moneytransfer.domain.exception.AccountNotActiveException;
import com.example.moneytransfer.domain.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link Account} business logic (debit / credit / isActive).
 * No Spring context is loaded — pure POJO tests.
 */
@DisplayName("Account – domain logic")
class AccountTest {

    private Account activeAccount;

    @BeforeEach
    void setUp() {
        activeAccount = Account.builder()
                .id(1L)
                .holderName("Alice Smith")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .version(0)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    // -----------------------------------------------------------------------
    // debit()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("debit – success: balance decreases by the exact amount")
    void testDebit_Success() {
        BigDecimal amount = new BigDecimal("250.00");

        activeAccount.debit(amount);

        assertThat(activeAccount.getBalance())
                .isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(activeAccount.getLastUpdated()).isNotNull();
    }

    @Test
    @DisplayName("debit – exact balance: debit entire balance leaves zero")
    void testDebit_ExactBalance() {
        activeAccount.debit(new BigDecimal("1000.00"));

        assertThat(activeAccount.getBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("debit – insufficient balance: throws InsufficientBalanceException")
    void testDebit_InsufficientBalance() {
        BigDecimal tooMuch = new BigDecimal("1500.00");

        assertThatThrownBy(() -> activeAccount.debit(tooMuch))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("1");           // account id
    }

    @Test
    @DisplayName("debit – locked account: throws AccountNotActiveException")
    void testDebit_LockedAccount() {
        activeAccount.setStatus(AccountStatus.LOCKED);

        assertThatThrownBy(() -> activeAccount.debit(new BigDecimal("100.00")))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    @DisplayName("debit – closed account: throws AccountNotActiveException")
    void testDebit_ClosedAccount() {
        activeAccount.setStatus(AccountStatus.CLOSED);

        assertThatThrownBy(() -> activeAccount.debit(new BigDecimal("100.00")))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    @DisplayName("debit – zero amount: throws IllegalArgumentException")
    void testDebit_ZeroAmount() {
        assertThatThrownBy(() -> activeAccount.debit(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("debit – null amount: throws IllegalArgumentException")
    void testDebit_NullAmount() {
        assertThatThrownBy(() -> activeAccount.debit(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -----------------------------------------------------------------------
    // credit()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("credit – success: balance increases by the exact amount")
    void testCredit_Success() {
        BigDecimal amount = new BigDecimal("500.00");

        activeAccount.credit(amount);

        assertThat(activeAccount.getBalance())
                .isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(activeAccount.getLastUpdated()).isNotNull();
    }

    @Test
    @DisplayName("credit – locked account: throws AccountNotActiveException")
    void testCredit_LockedAccount() {
        activeAccount.setStatus(AccountStatus.LOCKED);

        assertThatThrownBy(() -> activeAccount.credit(new BigDecimal("100.00")))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    @DisplayName("credit – zero amount: throws IllegalArgumentException")
    void testCredit_ZeroAmount() {
        assertThatThrownBy(() -> activeAccount.credit(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -----------------------------------------------------------------------
    // isActive()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("isActive – returns true for ACTIVE status")
    void testIsActive_ActiveAccount() {
        assertThat(activeAccount.isActive()).isTrue();
    }

    @Test
    @DisplayName("isActive – returns false for LOCKED status")
    void testIsActive_LockedAccount() {
        activeAccount.setStatus(AccountStatus.LOCKED);
        assertThat(activeAccount.isActive()).isFalse();
    }

    @Test
    @DisplayName("isActive – returns false for CLOSED status")
    void testIsActive_ClosedAccount() {
        activeAccount.setStatus(AccountStatus.CLOSED);
        assertThat(activeAccount.isActive()).isFalse();
    }
}
