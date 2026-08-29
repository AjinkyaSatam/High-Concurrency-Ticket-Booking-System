package com.ticketbooking.backend.service;

import com.ticketbooking.backend.dto.PaymentRequest;
import com.ticketbooking.backend.dto.PaymentResponse;
import com.ticketbooking.backend.entity.*;
import com.ticketbooking.backend.repository.BookingRepository;
import com.ticketbooking.backend.repository.PaymentRepository;
import com.ticketbooking.backend.repository.UserRepository;
import com.ticketbooking.backend.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User testUser;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        Role userRole = Role.builder().id(1L).name("ROLE_USER").build();
        testUser = User.builder()
                .id(1L)
                .email("alice@example.com")
                .fullName("Alice Walker")
                .roles(java.util.Set.of(userRole))
                .build();

        testBooking = Booking.builder()
                .id(50L)
                .bookingReference("BK-5050")
                .user(testUser)
                .status(BookingStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .build();
    }

    @Test
    @DisplayName("processPayment should throw IllegalArgumentException when Idempotency-Key is missing")
    void processPayment_MissingIdempotencyKey_ThrowsException() {
        PaymentRequest request = PaymentRequest.builder()
                .bookingId(50L)
                .paymentMethod("CREDIT_CARD")
                .build();

        assertThrows(IllegalArgumentException.class, () -> paymentService.processPayment("alice@example.com", "", request));
    }

    @Test
    @DisplayName("processPayment should return cached response when duplicate Idempotency-Key is provided")
    void processPayment_DuplicateIdempotencyKey_ReturnsCachedResponse() {
        PaymentRequest request = PaymentRequest.builder()
                .bookingId(50L)
                .paymentMethod("CREDIT_CARD")
                .build();

        Payment existingPayment = Payment.builder()
                .id(700L)
                .idempotencyKey("IDEM-12345")
                .transactionRef("TXN-EXISTING")
                .booking(testBooking)
                .user(testUser)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.SUCCESS)
                .createdAt(OffsetDateTime.now())
                .build();

        when(paymentRepository.findByIdempotencyKey("IDEM-12345")).thenReturn(Optional.of(existingPayment));

        PaymentResponse response = paymentService.processPayment("alice@example.com", "IDEM-12345", request);

        assertNotNull(response);
        assertTrue(response.isDuplicateRequest());
        assertEquals("TXN-EXISTING", response.getTransactionRef());
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("processPayment should complete payment successfully for new idempotency key")
    void processPayment_Success() {
        PaymentRequest request = PaymentRequest.builder()
                .bookingId(50L)
                .paymentMethod("CREDIT_CARD")
                .build();

        when(paymentRepository.findByIdempotencyKey("IDEM-NEW-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(testUser));
        when(bookingRepository.findByIdAndUserId(50L, 1L)).thenReturn(Optional.of(testBooking));

        Payment savedPayment = Payment.builder()
                .id(701L)
                .idempotencyKey("IDEM-NEW-123")
                .transactionRef("TXN-SUCCESS99")
                .booking(testBooking)
                .user(testUser)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.SUCCESS)
                .createdAt(OffsetDateTime.now())
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentResponse response = paymentService.processPayment("alice@example.com", "IDEM-NEW-123", request);

        assertNotNull(response);
        assertFalse(response.isDuplicateRequest());
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(BookingStatus.CONFIRMED, testBooking.getStatus());
        verify(bookingRepository).save(testBooking);
    }
}
