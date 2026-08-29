package com.ticketbooking.backend.service.impl;

import com.ticketbooking.backend.dto.*;
import com.ticketbooking.backend.entity.*;
import com.ticketbooking.backend.repository.*;
import com.ticketbooking.backend.service.BookingService;
import com.ticketbooking.backend.service.DistributedLockService;
import com.ticketbooking.backend.service.SeatEventPublisher;
import com.ticketbooking.backend.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatHoldServiceImpl implements SeatHoldService {

    private final SeatHoldRepository seatHoldRepository;
    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final BookingService bookingService;
    private final DistributedLockService distributedLockService;
    private final SeatEventPublisher seatEventPublisher;
    private final RedissonClient redissonClient;

    @Override
    public SeatHoldResponse createHold(String userEmail, SeatHoldRequest request) {
        return distributedLockService.executeWithSeatLocks(request.getSeatIds(), 5, 10, () -> processHoldTransaction(userEmail, request));
    }

    @Transactional
    public SeatHoldResponse processHoldTransaction(String userEmail, SeatHoldRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + request.getEventId()));

        List<Seat> seats = seatRepository.findByEventIdAndIdInWithLock(request.getEventId(), request.getSeatIds());
        if (seats.size() != request.getSeatIds().size()) {
            throw new IllegalArgumentException("One or more requested seats do not exist");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new IllegalStateException("Seat " + seat.getSeatCode() + " is currently " + seat.getStatus());
            }
            seat.setStatus(SeatStatus.HELD);
            totalAmount = totalAmount.add(seat.getPrice());
        }
        seatRepository.saveAll(seats);

        String holdRef = "HOLD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(10);

        SeatHold seatHold = SeatHold.builder()
                .holdReference(holdRef)
                .user(user)
                .event(event)
                .status(SeatHoldStatus.ACTIVE)
                .expiresAt(expiresAt)
                .totalAmount(totalAmount)
                .seats(seats)
                .build();

        SeatHold savedHold = seatHoldRepository.save(seatHold);

        // Update available seats
        long available = seatRepository.countByEventIdAndStatus(event.getId(), SeatStatus.AVAILABLE);
        event.setAvailableSeats((int) available);
        eventRepository.save(event);

        return mapToSeatHoldResponse(savedHold);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatHoldResponse getHoldByReference(String userEmail, String holdReference) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        SeatHold seatHold = seatHoldRepository.findByHoldReferenceAndUserId(holdReference, user.getId())
                .orElseThrow(() -> new RuntimeException("Hold reference not found or unauthorized"));

        return mapToSeatHoldResponse(seatHold);
    }

    @Override
    @Transactional
    public BookingResponse confirmHoldToBooking(String userEmail, String holdReference) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        SeatHold seatHold = seatHoldRepository.findByHoldReferenceAndUserId(holdReference, user.getId())
                .orElseThrow(() -> new RuntimeException("Hold reference not found or unauthorized"));

        if (seatHold.getStatus() != SeatHoldStatus.ACTIVE) {
            throw new IllegalStateException("Seat hold is no longer active (Status: " + seatHold.getStatus() + ")");
        }

        if (seatHold.getExpiresAt().isBefore(OffsetDateTime.now())) {
            seatHold.setStatus(SeatHoldStatus.EXPIRED);
            for (Seat s : seatHold.getSeats()) {
                if (s.getStatus() == SeatStatus.HELD) s.setStatus(SeatStatus.AVAILABLE);
            }
            seatHoldRepository.save(seatHold);
            throw new IllegalStateException("Seat hold has expired. Please select your seats again.");
        }

        // Revert HELD status back to AVAILABLE temporarily so createBooking can transition them to BOOKED cleanly
        List<Long> seatIds = seatHold.getSeats().stream().map(Seat::getId).collect(Collectors.toList());
        for (Seat s : seatHold.getSeats()) {
            s.setStatus(SeatStatus.AVAILABLE);
        }
        seatRepository.saveAll(seatHold.getSeats());

        BookingRequest bookingRequest = BookingRequest.builder()
                .eventId(seatHold.getEvent().getId())
                .seatIds(seatIds)
                .build();

        BookingResponse bookingResponse = bookingService.createBooking(userEmail, bookingRequest);

        seatHold.setStatus(SeatHoldStatus.CONFIRMED);
        seatHoldRepository.save(seatHold);

        return bookingResponse;
    }

    @Override
    @Scheduled(fixedRate = 10000) // Runs every 10 seconds
    public void releaseExpiredHolds() {
        RLock lock = redissonClient.getLock("lock:sweeper:seat-holds");
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, 15, TimeUnit.SECONDS);
            if (!acquired) {
                log.debug("Another node is currently executing expired seat hold release sweeper. Skipping execution.");
                return;
            }
            processExpiredHoldsRelease();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Expired hold release sweeper interrupted", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    public void processExpiredHoldsRelease() {
        OffsetDateTime now = OffsetDateTime.now();
        List<SeatHold> expiredHolds = seatHoldRepository.findByStatusAndExpiresAtBefore(SeatHoldStatus.ACTIVE, now);

        if (!expiredHolds.isEmpty()) {
            log.info("Found {} expired seat holds to clean up...", expiredHolds.size());
            for (SeatHold hold : expiredHolds) {
                hold.setStatus(SeatHoldStatus.EXPIRED);
                List<SeatResponse> releasedSeatResponses = new ArrayList<>();
                for (Seat seat : hold.getSeats()) {
                    if (seat.getStatus() == SeatStatus.HELD) {
                        seat.setStatus(SeatStatus.AVAILABLE);
                        releasedSeatResponses.add(SeatResponse.builder()
                                .id(seat.getId())
                                .eventId(seat.getEvent().getId())
                                .sectionName(seat.getSectionName())
                                .rowName(seat.getRowName())
                                .seatNumber(seat.getSeatNumber())
                                .seatCode(seat.getSeatCode())
                                .price(seat.getPrice())
                                .status(SeatStatus.AVAILABLE)
                                .build());
                    }
                }
                seatHoldRepository.save(hold);

                // Update available seats count for event
                Event event = hold.getEvent();
                long remaining = seatRepository.countByEventIdAndStatus(event.getId(), SeatStatus.AVAILABLE);
                event.setAvailableSeats((int) remaining);
                eventRepository.save(event);

                // Publish live WebSocket notification for released seats
                if (!releasedSeatResponses.isEmpty()) {
                    seatEventPublisher.publishSeatUpdates(event.getId(), releasedSeatResponses);
                }
            }
            log.info("Successfully released expired seat holds and published WebSocket updates.");
        }
    }

    private SeatHoldResponse mapToSeatHoldResponse(SeatHold hold) {
        long remainingSec = Math.max(0, Duration.between(OffsetDateTime.now(), hold.getExpiresAt()).getSeconds());

        List<SeatResponse> seatResponses = hold.getSeats().stream().map(s -> SeatResponse.builder()
                .id(s.getId())
                .eventId(s.getEvent().getId())
                .sectionName(s.getSectionName())
                .rowName(s.getRowName())
                .seatNumber(s.getSeatNumber())
                .seatCode(s.getSeatCode())
                .price(s.getPrice())
                .status(s.getStatus())
                .build()).collect(Collectors.toList());

        return SeatHoldResponse.builder()
                .id(hold.getId())
                .holdReference(hold.getHoldReference())
                .eventId(hold.getEvent().getId())
                .eventTitle(hold.getEvent().getTitle())
                .status(hold.getStatus())
                .expiresAt(hold.getExpiresAt())
                .remainingSeconds(remainingSec)
                .totalAmount(hold.getTotalAmount())
                .seats(seatResponses)
                .build();
    }
}
