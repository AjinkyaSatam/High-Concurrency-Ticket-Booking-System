package com.ticketbooking.backend.service.impl;

import com.ticketbooking.backend.dto.PaymentRequest;
import com.ticketbooking.backend.dto.PaymentResponse;
import com.ticketbooking.backend.entity.Booking;
import com.ticketbooking.backend.entity.BookingStatus;
import com.ticketbooking.backend.entity.Payment;
import com.ticketbooking.backend.entity.PaymentStatus;
import com.ticketbooking.backend.entity.User;
import com.ticketbooking.backend.repository.BookingRepository;
import com.ticketbooking.backend.repository.PaymentRepository;
import com.ticketbooking.backend.repository.UserRepository;
import com.ticketbooking.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PaymentResponse processPayment(String userEmail, String idempotencyKey, PaymentRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required for payment processing");
        }

        // Idempotency check: if payment with this key exists, return stored result
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            log.info("Idempotency match found for key: {}. Returning cached payment result.", idempotencyKey);
            Payment payment = existingPayment.get();
            PaymentResponse response = mapToPaymentResponse(payment);
            response.setDuplicateRequest(true);
            return response;
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        Booking booking = bookingRepository.findByIdAndUserId(request.getBookingId(), user.getId())
                .orElseThrow(() -> new RuntimeException("Booking not found or unauthorized"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Cannot process payment for cancelled booking");
        }

        String txnRef = "TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        String method = request.getPaymentMethod() != null ? request.getPaymentMethod() : "CREDIT_CARD";

        Payment payment = Payment.builder()
                .idempotencyKey(idempotencyKey)
                .transactionRef(txnRef)
                .booking(booking)
                .user(user)
                .amount(booking.getTotalAmount())
                .paymentMethod(method)
                .status(PaymentStatus.SUCCESS)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        return mapToPaymentResponse(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(String userEmail, Long bookingId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("No payment found for booking ID: " + bookingId));

        if (!payment.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to view payment");
        }

        return mapToPaymentResponse(payment);
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .idempotencyKey(payment.getIdempotencyKey())
                .transactionRef(payment.getTransactionRef())
                .bookingId(payment.getBooking().getId())
                .bookingReference(payment.getBooking().getBookingReference())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .isDuplicateRequest(false)
                .build();
    }
}
