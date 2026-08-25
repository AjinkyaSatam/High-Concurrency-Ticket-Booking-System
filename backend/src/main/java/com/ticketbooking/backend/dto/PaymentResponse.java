package com.ticketbooking.backend.dto;

import com.ticketbooking.backend.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private Long id;
    private String idempotencyKey;
    private String transactionRef;
    private Long bookingId;
    private String bookingReference;
    private BigDecimal amount;
    private String paymentMethod;
    private PaymentStatus status;
    private OffsetDateTime createdAt;
    private boolean isDuplicateRequest;
}
