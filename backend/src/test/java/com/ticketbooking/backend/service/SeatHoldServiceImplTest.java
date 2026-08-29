package com.ticketbooking.backend.service;

import com.ticketbooking.backend.dto.SeatHoldRequest;
import com.ticketbooking.backend.dto.SeatHoldResponse;
import com.ticketbooking.backend.entity.*;
import com.ticketbooking.backend.repository.EventRepository;
import com.ticketbooking.backend.repository.SeatHoldRepository;
import com.ticketbooking.backend.repository.SeatRepository;
import com.ticketbooking.backend.repository.UserRepository;
import com.ticketbooking.backend.service.impl.SeatHoldServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatHoldServiceImplTest {

    @Mock
    private SeatHoldRepository seatHoldRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingService bookingService;
    @Mock
    private DistributedLockService distributedLockService;
    @Mock
    private SeatEventPublisher seatEventPublisher;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock rLock;

    @InjectMocks
    private SeatHoldServiceImpl seatHoldService;

    private User testUser;
    private Event testEvent;
    private Seat testSeat;

    @BeforeEach
    void setUp() {
        Role userRole = Role.builder().id(1L).name("ROLE_USER").build();
        testUser = User.builder()
                .id(1L)
                .email("john@example.com")
                .fullName("John Doe")
                .roles(java.util.Set.of(userRole))
                .build();

        testEvent = Event.builder()
                .id(100L)
                .title("Coldplay Music of the Spheres World Tour")
                .availableSeats(50)
                .build();

        testSeat = Seat.builder()
                .id(10L)
                .seatCode("SEC-A-R1-S1")
                .price(new BigDecimal("150.00"))
                .status(SeatStatus.AVAILABLE)
                .event(testEvent)
                .build();
    }

    @Test
    @DisplayName("processHoldTransaction should hold seats and return SeatHoldResponse")
    void processHoldTransaction_Success() {
        SeatHoldRequest request = SeatHoldRequest.builder()
                .eventId(100L)
                .seatIds(List.of(10L))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(100L)).thenReturn(Optional.of(testEvent));
        when(seatRepository.findByEventIdAndIdInWithLock(100L, List.of(10L))).thenReturn(List.of(testSeat));
        when(seatRepository.countByEventIdAndStatus(100L, SeatStatus.AVAILABLE)).thenReturn(49L);

        SeatHold savedHold = SeatHold.builder()
                .id(500L)
                .holdReference("HOLD-TEST1234")
                .user(testUser)
                .event(testEvent)
                .status(SeatHoldStatus.ACTIVE)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .totalAmount(new BigDecimal("150.00"))
                .seats(List.of(testSeat))
                .build();

        when(seatHoldRepository.save(any(SeatHold.class))).thenReturn(savedHold);

        SeatHoldResponse response = seatHoldService.processHoldTransaction("john@example.com", request);

        assertNotNull(response);
        assertEquals("HOLD-TEST1234", response.getHoldReference());
        assertEquals(SeatHoldStatus.ACTIVE, response.getStatus());
        assertEquals(SeatStatus.HELD, testSeat.getStatus());
        verify(seatRepository).saveAll(List.of(testSeat));
    }

    @Test
    @DisplayName("processHoldTransaction should throw IllegalStateException when seat is not AVAILABLE")
    void processHoldTransaction_SeatAlreadyHeld_ThrowsException() {
        testSeat.setStatus(SeatStatus.HELD);
        SeatHoldRequest request = SeatHoldRequest.builder()
                .eventId(100L)
                .seatIds(List.of(10L))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(eventRepository.findById(100L)).thenReturn(Optional.of(testEvent));
        when(seatRepository.findByEventIdAndIdInWithLock(100L, List.of(10L))).thenReturn(List.of(testSeat));

        assertThrows(IllegalStateException.class, () -> seatHoldService.processHoldTransaction("john@example.com", request));
    }

    @Test
    @DisplayName("releaseExpiredHolds should release held seats and publish WebSocket updates")
    void releaseExpiredHolds_Success() throws InterruptedException {
        when(redissonClient.getLock("lock:sweeper:seat-holds")).thenReturn(rLock);
        when(rLock.tryLock(eq(0L), eq(15L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        testSeat.setStatus(SeatStatus.HELD);
        SeatHold expiredHold = SeatHold.builder()
                .id(501L)
                .holdReference("HOLD-EXPIRED")
                .user(testUser)
                .event(testEvent)
                .status(SeatHoldStatus.ACTIVE)
                .expiresAt(OffsetDateTime.now().minusMinutes(5))
                .seats(List.of(testSeat))
                .build();

        when(seatHoldRepository.findByStatusAndExpiresAtBefore(eq(SeatHoldStatus.ACTIVE), any(OffsetDateTime.class)))
                .thenReturn(List.of(expiredHold));

        seatHoldService.releaseExpiredHolds();

        assertEquals(SeatHoldStatus.EXPIRED, expiredHold.getStatus());
        assertEquals(SeatStatus.AVAILABLE, testSeat.getStatus());
        verify(seatHoldRepository).save(expiredHold);
        verify(seatEventPublisher).publishSeatUpdates(eq(100L), anyList());
        verify(rLock).unlock();
    }
}
