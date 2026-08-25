package com.ticketbooking.backend.controller;

import com.ticketbooking.backend.dto.PaymentRequest;
import com.ticketbooking.backend.dto.PaymentResponse;
import com.ticketbooking.backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(
            Principal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.processPayment(principal.getName(), idempotencyKey, request));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBooking(Principal principal, @PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(principal.getName(), bookingId));
    }
}
