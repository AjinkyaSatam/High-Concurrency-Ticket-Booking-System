package com.ticketbooking.backend.service;

import com.ticketbooking.backend.BackendApplication;
import com.ticketbooking.backend.dto.BookingRequest;
import com.ticketbooking.backend.dto.BookingResponse;
import com.ticketbooking.backend.entity.*;
import com.ticketbooking.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = BackendApplication.class)
@ActiveProfiles("test")
public class HighConcurrencyBookingTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Event testEvent;
    private Seat testSeat;
    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        seatRepository.deleteAll();
        eventRepository.deleteAll();
        venueRepository.deleteAll();
        userRepository.deleteAll();

        // Create test venue
        Venue venue = venueRepository.save(Venue.builder()
                .name("Grand Arena")
                .city("Metropolis")
                .address("100 Main St")
                .capacity(1000)
                .build());

        // Create test event
        testEvent = eventRepository.save(Event.builder()
                .title("High Concurrency Concert")
                .category("Music")
                .eventDate(OffsetDateTime.now().plusDays(5))
                .status(EventStatus.ON_SALE)
                .venue(venue)
                .totalSeats(10)
                .availableSeats(10)
                .build());

        // Create target seat for collision test
        testSeat = seatRepository.save(Seat.builder()
                .event(testEvent)
                .sectionName("VIP")
                .rowName("A")
                .seatNumber(1)
                .seatCode("A-1")
                .price(new BigDecimal("150.00"))
                .status(SeatStatus.AVAILABLE)
                .build());

        // Create 20 distinct users for 20 concurrent threads
        testUsers = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            testUsers.add(userRepository.save(User.builder()
                    .email("user" + i + "@test.com")
                    .password("password")
                    .fullName("Test User " + i)
                    .build()));
        }
    }

    @Test
    @DisplayName("20 Concurrent threads trying to book the exact same seat simultaneously -> Exactly 1 succeeds")
    void testConcurrentSeatBookingConflictResolution() throws InterruptedException {
        int numberOfThreads = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> errorMessages = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfThreads; i++) {
            final String userEmail = testUsers.get(i).getEmail();
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Hold all threads until signal
                    BookingRequest request = BookingRequest.builder()
                            .eventId(testEvent.getId())
                            .seatIds(List.of(testSeat.getId()))
                            .build();

                    BookingResponse response = bookingService.createBooking(userEmail, request);
                    if (response != null && response.getStatus() == BookingStatus.CONFIRMED) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    errorMessages.add(e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown(); // Release all 20 threads simultaneously
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);

        executorService.shutdown();

        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(1, successCount.get(), "Exactly one user should successfully book the seat");
        assertEquals(19, failureCount.get(), "19 users should fail due to lock collision/seat booked");

        // Verify database state
        Seat updatedSeat = seatRepository.findById(testSeat.getId()).orElseThrow();
        assertEquals(SeatStatus.BOOKED, updatedSeat.getStatus(), "Seat status must be BOOKED");

        List<Booking> allBookings = bookingRepository.findAll();
        assertEquals(1, allBookings.size(), "Only one booking record must exist in DB");
    }
}
