package com.example.moneytransfer.domain.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TransferRequest} Bean Validation annotations.
 * No Spring context – uses the default Hibernate Validator directly.
 */
@DisplayName("TransferRequest – Bean Validation")
class TransferRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // -----------------------------------------------------------------------
    // Valid request
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("valid request: no constraint violations")
    void testValidRequest() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .idempotencyKey("uuid-1234-abcd")
                .build();

        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Invalid amount
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("invalid amount – zero: should fail @DecimalMin")
    void testInvalidAmount_Zero() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(BigDecimal.ZERO)
                .idempotencyKey("uuid-1234-abcd")
                .build();

        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    @Test
    @DisplayName("invalid amount – negative: should fail @DecimalMin")
    void testInvalidAmount_Negative() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(new BigDecimal("-50.00"))
                .idempotencyKey("uuid-1234-abcd")
                .build();

        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    // -----------------------------------------------------------------------
    // Null fields
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("null fromAccountId: should fail @NotNull")
    void testNullFromAccountId() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(null)
                .toAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .idempotencyKey("uuid-1234-abcd")
                .build();

        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("fromAccountId"));
    }

    @Test
    @DisplayName("null toAccountId: should fail @NotNull")
    void testNullToAccountId() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(1L)
                .toAccountId(null)
                .amount(new BigDecimal("100.00"))
                .idempotencyKey("uuid-1234-abcd")
                .build();

        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("toAccountId"));
    }

    @Test
    @DisplayName("null amount: should fail @NotNull")
    void testNullAmount() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(null)
                .idempotencyKey("uuid-1234-abcd")
                .build();

        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    @Test
    @DisplayName("blank idempotencyKey: should fail @NotBlank")
    void testBlankIdempotencyKey() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(new BigDecimal("100.00"))
                .idempotencyKey("   ")
                .build();

        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("idempotencyKey"));
    }

    @Test
    @DisplayName("all null fields: should produce multiple violations")
    void testAllNullFields() {
        TransferRequest request = new TransferRequest();

        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(request);

        // fromAccountId, toAccountId, amount, idempotencyKey — at minimum 4 violations
        assertThat(violations.size()).isGreaterThanOrEqualTo(4);
    }
}
