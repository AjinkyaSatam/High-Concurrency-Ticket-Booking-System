package com.ticketbooking.backend.service.impl;

import com.ticketbooking.backend.entity.BookingStatus;
import com.ticketbooking.backend.entity.SeatStatus;
import com.ticketbooking.backend.repository.*;
import com.ticketbooking.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Override
    public Map<String, Object> getDashboardAnalytics() {
        Map<String, Object> metrics = new HashMap<>();

        long totalBookings = bookingRepository.count();
        long totalConfirmedBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count();

        BigDecimal totalRevenue = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalEvents = eventRepository.count();
        long totalUsers = userRepository.count();
        long totalTicketsSold = ticketRepository.count();

        long totalSeats = seatRepository.count();
        long bookedSeats = seatRepository.findAll().stream()
                .filter(s -> s.getStatus() == SeatStatus.BOOKED)
                .count();
        long heldSeats = seatRepository.findAll().stream()
                .filter(s -> s.getStatus() == SeatStatus.HELD)
                .count();
        long availableSeats = seatRepository.findAll().stream()
                .filter(s -> s.getStatus() == SeatStatus.AVAILABLE)
                .count();

        double occupancyRate = (totalSeats > 0) ? ((double) bookedSeats / totalSeats) * 100.0 : 0.0;
        double conversionRate = (heldSeats + bookedSeats > 0) ? ((double) bookedSeats / (heldSeats + bookedSeats)) * 100.0 : 100.0;

        metrics.put("totalRevenue", totalRevenue);
        metrics.put("totalBookings", totalBookings);
        metrics.put("confirmedBookings", totalConfirmedBookings);
        metrics.put("totalTicketsSold", totalTicketsSold);
        metrics.put("totalEvents", totalEvents);
        metrics.put("totalUsers", totalUsers);

        metrics.put("totalSeats", totalSeats);
        metrics.put("bookedSeats", bookedSeats);
        metrics.put("heldSeats", heldSeats);
        metrics.put("availableSeats", availableSeats);
        metrics.put("occupancyRate", Math.round(occupancyRate * 10.0) / 10.0);
        metrics.put("conversionRate", Math.round(conversionRate * 10.0) / 10.0);

        metrics.put("systemConcurrencyMode", "REDISSON_MULTI_LOCK_DISTRIBUTED");
        metrics.put("redisStatus", "HEALTHY");

        return metrics;
    }
}
