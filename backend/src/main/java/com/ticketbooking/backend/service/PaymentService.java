package com.ticketbooking.backend.service;

import com.ticketbooking.backend.dto.PaymentRequest;
import com.ticketbooking.backend.dto.PaymentResponse;

public interface PaymentService {
    PaymentResponse processPayment(String userEmail, String idempotencyKey, PaymentRequest request);
    PaymentResponse getPaymentByBookingId(String userEmail, Long bookingId);
}
