package com.ticketbooking.backend.service;

import com.ticketbooking.backend.dto.BookingRequest;
import com.ticketbooking.backend.dto.BookingResponse;
import com.ticketbooking.backend.entity.*;
import com.ticketbooking.backend.repository.*;
import com.ticketbooking.backend.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DistributedLockService distributedLockService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User testUser;
    private Event testEvent;
    private Seat testSeat;

    @BeforeEach
    void setUp() {
        Role userRole = Role.builder().id(1L).name("ROLE_USER").build();
        testUser = User.builder()
                .id(1L)
                .email("jane@example.com")
                .fullName("Jane Smith")
                .roles(java.util.Set.of(userRole))
                .build();

        Venue venue = Venue.builder().id(1L).name("Royal Albert Hall").city("London").build();
        testEvent = Event.builder()
                .id(200L)
                .title("Taylor Swift Eras Tour")
                .venue(venue)
                .status(EventStatus.UPCOMING)
                .availableSeats(100)
                .build();

        testSeat = Seat.builder()
                .id(20L)
                .seatCode("SEC-VIP-R1-S5")
                .price(new BigDecimal("250.00"))
                .status(SeatStatus.AVAILABLE)
                .event(testEvent)
                .build();
    }

    @Test
    @DisplayName("processBookingTransaction should book seats and return BookingResponse")
    void processBookingTransaction_Success() {
        BookingRequest request = BookingRequest.builder()
                .eventId(200L)
                .seatIds(List.of(20L))
                .build();

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(200L)).thenReturn(Optional.of(testEvent));
        when(seatRepository.findByEventIdAndIdInWithLock(200L, List.of(20L))).thenReturn(List.of(testSeat));
        when(seatRepository.countByEventIdAndStatus(200L, SeatStatus.AVAILABLE)).thenReturn(99L);

        Booking savedBooking = Booking.builder()
                .id(1001L)
                .bookingReference("BK-TEST9999")
                .user(testUser)
                .event(testEvent)
                .status(BookingStatus.PENDING)
                .totalAmount(new BigDecimal("250.00"))
                .createdAt(OffsetDateTime.now())
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.processBookingTransaction("jane@example.com", request);

        assertNotNull(response);
        assertEquals("BK-TEST9999", response.getBookingReference());
        assertEquals(BookingStatus.PENDING, response.getStatus());
        assertEquals(SeatStatus.BOOKED, testSeat.getStatus());
        verify(seatRepository).saveAll(List.of(testSeat));
    }

    @Test
    @DisplayName("processBookingTransaction should throw RuntimeException when user is not found")
    void processBookingTransaction_UserNotFound_ThrowsException() {
        BookingRequest request = BookingRequest.builder()
                .eventId(200L)
                .seatIds(List.of(20L))
                .build();

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> bookingService.processBookingTransaction("unknown@example.com", request));
    }
}
