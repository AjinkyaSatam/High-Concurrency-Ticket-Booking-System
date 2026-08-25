package com.ticketbooking.backend.service.impl;

import com.ticketbooking.backend.dto.BookingRequest;
import com.ticketbooking.backend.dto.BookingResponse;
import com.ticketbooking.backend.dto.TicketResponse;
import com.ticketbooking.backend.entity.*;
import com.ticketbooking.backend.repository.*;
import com.ticketbooking.backend.service.BookingService;
import com.ticketbooking.backend.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final DistributedLockService distributedLockService;

    @Override
    public BookingResponse createBooking(String userEmail, BookingRequest request) {
        // Acquire Redis distributed multi-lock across all requested seats first (prevents cross-node race conditions)
        return distributedLockService.executeWithSeatLocks(request.getSeatIds(), 5, 10, () -> processBookingTransaction(userEmail, request));
    }

    @Transactional
    public BookingResponse processBookingTransaction(String userEmail, BookingRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + request.getEventId()));

        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED) {
            throw new IllegalStateException("Cannot book tickets for event status: " + event.getStatus());
        }

        // Use pessimistic write lock at database level as secondary safeguard
        List<Seat> seats = seatRepository.findByEventIdAndIdInWithLock(request.getEventId(), request.getSeatIds());
        if (seats.size() != request.getSeatIds().size()) {
            throw new IllegalArgumentException("One or more selected seats do not exist for this event");
        }

        // Validate seat availability
        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new IllegalStateException("Seat " + seat.getSeatCode() + " is already " + seat.getStatus());
            }
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Reserve seats and sum price
        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);
            totalAmount = totalAmount.add(seat.getPrice());
        }
        seatRepository.saveAll(seats);

        // Generate Booking
        String bookingRef = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Booking booking = Booking.builder()
                .bookingReference(bookingRef)
                .user(user)
                .event(event)
                .totalAmount(totalAmount)
                .status(BookingStatus.CONFIRMED)
                .bookingTime(OffsetDateTime.now())
                .tickets(new ArrayList<>())
                .build();

        // Generate Tickets
        for (Seat seat : seats) {
            String ticketCode = "TCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Ticket ticket = Ticket.builder()
                    .booking(booking)
                    .seat(seat)
                    .ticketCode(ticketCode)
                    .price(seat.getPrice())
                    .build();
            booking.getTickets().add(ticket);
        }

        Booking savedBooking = bookingRepository.save(booking);

        // Update event available seats count
        long remainingSeats = seatRepository.countByEventIdAndStatus(event.getId(), SeatStatus.AVAILABLE);
        event.setAvailableSeats((int) remainingSeats);
        eventRepository.save(event);

        return mapToBookingResponse(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        List<Booking> bookings = bookingRepository.findByUserIdOrderByBookingTimeDesc(user.getId());
        return bookings.stream().map(this::mapToBookingResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(String userEmail, Long bookingId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        Booking booking = bookingRepository.findByIdAndUserId(bookingId, user.getId())
                .orElseThrow(() -> new RuntimeException("Booking not found or unauthorized"));

        return mapToBookingResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(String userEmail, Long bookingId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        Booking booking = bookingRepository.findByIdAndUserId(bookingId, user.getId())
                .orElseThrow(() -> new RuntimeException("Booking not found or unauthorized"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        // Release seats
        List<Seat> seatsToRelease = new ArrayList<>();
        for (Ticket ticket : booking.getTickets()) {
            Seat seat = ticket.getSeat();
            seat.setStatus(SeatStatus.AVAILABLE);
            seatsToRelease.add(seat);
        }
        seatRepository.saveAll(seatsToRelease);

        Booking updatedBooking = bookingRepository.save(booking);

        // Update event available seats count
        Event event = booking.getEvent();
        long remainingSeats = seatRepository.countByEventIdAndStatus(event.getId(), SeatStatus.AVAILABLE);
        event.setAvailableSeats((int) remainingSeats);
        eventRepository.save(event);

        return mapToBookingResponse(updatedBooking);
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        List<TicketResponse> ticketResponses = booking.getTickets().stream()
                .map(ticket -> TicketResponse.builder()
                        .id(ticket.getId())
                        .ticketCode(ticket.getTicketCode())
                        .seatId(ticket.getSeat().getId())
                        .sectionName(ticket.getSeat().getSectionName())
                        .rowName(ticket.getSeat().getRowName())
                        .seatNumber(ticket.getSeat().getSeatNumber())
                        .seatCode(ticket.getSeat().getSeatCode())
                        .price(ticket.getPrice())
                        .build())
                .collect(Collectors.toList());

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .userId(booking.getUser().getId())
                .userEmail(booking.getUser().getEmail())
                .userName(booking.getUser().getFullName())
                .eventId(booking.getEvent().getId())
                .eventTitle(booking.getEvent().getTitle())
                .venueName(booking.getEvent().getVenue().getName())
                .eventDate(booking.getEvent().getEventDate())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .bookingTime(booking.getBookingTime())
                .tickets(ticketResponses)
                .build();
    }
}
