package com.example.moneytransfer.controller;

import com.example.moneytransfer.domain.dto.AccountResponse;
import com.example.moneytransfer.domain.dto.TransferResponse;
import com.example.moneytransfer.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * GET /api/v1/accounts/{id}
     * Returns full account details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    /**
     * GET /api/v1/accounts/{id}/balance
     * Returns only the current balance.
     */
    @GetMapping("/{id}/balance")
    public ResponseEntity<Map<String, BigDecimal>> getBalance(@PathVariable Long id) {
        BigDecimal balance = accountService.getBalance(id);
        return ResponseEntity.ok(Map.of("balance", balance));
    }

    /**
     * GET /api/v1/accounts/{id}/transactions?page=0&size=20
     * Returns paginated transaction history (sent + received).
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<Page<TransferResponse>> getTransactions(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "createdOn") Pageable pageable) {
        return ResponseEntity.ok(accountService.getTransactions(id, pageable));
    }
}
