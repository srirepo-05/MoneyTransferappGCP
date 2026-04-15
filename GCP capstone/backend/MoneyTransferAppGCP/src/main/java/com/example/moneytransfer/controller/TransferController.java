package com.example.moneytransfer.controller;

import com.example.moneytransfer.domain.dto.TransferRequest;
import com.example.moneytransfer.domain.dto.TransferResponse;
import com.example.moneytransfer.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /**
     * POST /api/v1/transfers
     * Execute a fund transfer between two accounts.
     *
     * @param request validated transfer payload
     * @return 201 CREATED with transfer receipt
     */
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferService.transfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
