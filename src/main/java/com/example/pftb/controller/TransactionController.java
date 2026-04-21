package com.example.pftb.controller;

import com.example.pftb.dto.TransactionDto;
import com.example.pftb.dto.TransactionRequest;
import com.example.pftb.service.TransactionService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionDto>> getTransactions(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(transactionService.getTransactionsByUser(username));
    }

    @PostMapping
    public ResponseEntity<TransactionDto> createTransaction(
            Authentication authentication,
            @Valid @RequestBody TransactionRequest request
    ) {
        String username = authentication.getName();
        return ResponseEntity.ok(transactionService.createTransaction(username, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            Authentication authentication,
            @PathVariable Long id
    ) {
        String username = authentication.getName();
        transactionService.deleteTransaction(username, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    public void exportTransactions(Authentication authentication, HttpServletResponse response) throws IOException {
        String username = authentication.getName();
        
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.csv");
        
        transactionService.exportCsvToStream(username, response.getWriter());
    }
}