package com.example.pftb.controller;

import com.example.pftb.dto.PaymentObligationRequest;
import com.example.pftb.dto.PaymentObligationResponse;
import com.example.pftb.dto.PaymentObligationSummaryResponse;
import com.example.pftb.dto.RecordPaymentRequest;
import com.example.pftb.entity.PaymentObligationPriority;
import com.example.pftb.entity.PaymentObligationStatus;
import com.example.pftb.entity.PaymentObligationType;
import com.example.pftb.service.PaymentObligationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payment-obligations")
public class PaymentObligationController {
    private final PaymentObligationService paymentObligationService;

    public PaymentObligationController(PaymentObligationService paymentObligationService) {
        this.paymentObligationService = paymentObligationService;
    }

    @GetMapping
    public ResponseEntity<List<PaymentObligationResponse>> list(Authentication authentication,
                                                                @RequestParam(required = false) PaymentObligationType type,
                                                                @RequestParam(required = false) PaymentObligationStatus status,
                                                                @RequestParam(required = false) PaymentObligationPriority priority,
                                                                @RequestParam(required = false) LocalDate dueBefore,
                                                                @RequestParam(required = false) LocalDate dueAfter) {
        return ResponseEntity.ok(paymentObligationService.list(authentication.getName(), type, status, priority, dueBefore, dueAfter));
    }

    @PostMapping
    public ResponseEntity<PaymentObligationResponse> create(Authentication authentication, @Valid @RequestBody PaymentObligationRequest request) {
        return ResponseEntity.ok(paymentObligationService.create(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentObligationResponse> update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody PaymentObligationRequest request) {
        return ResponseEntity.ok(paymentObligationService.update(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        paymentObligationService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/record-payment")
    public ResponseEntity<PaymentObligationResponse> recordPayment(Authentication authentication,
                                                                   @PathVariable Long id,
                                                                   @Valid @RequestBody RecordPaymentRequest request) {
        return ResponseEntity.ok(paymentObligationService.recordPayment(authentication.getName(), id, request));
    }

    @GetMapping("/summary")
    public ResponseEntity<PaymentObligationSummaryResponse> summary(Authentication authentication) {
        return ResponseEntity.ok(paymentObligationService.summary(authentication.getName()));
    }
}
