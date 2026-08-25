package com.ticketbooking.backend.service.impl;

import com.ticketbooking.backend.dto.SeatGenerationRequest;
import com.ticketbooking.backend.dto.SeatResponse;
import com.ticketbooking.backend.entity.Event;
import com.ticketbooking.backend.entity.Seat;
import com.ticketbooking.backend.entity.SeatStatus;
import com.ticketbooking.backend.repository.EventRepository;
import com.ticketbooking.backend.repository.SeatRepository;
import com.ticketbooking.backend.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public List<SeatResponse> getSeatsByEventId(Long eventId) {
        List<Seat> seats = seatRepository.findByEventIdOrderBySectionNameAscRowNameAscSeatNumberAsc(eventId);
        if (seats.isEmpty()) {
            return autoGenerateSeatsIfEmpty(eventId);
        }
        return seats.stream().map(this::mapToSeatResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<SeatResponse> generateSeatsForEvent(Long eventId, SeatGenerationRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + eventId));

        // Clear existing seats
        seatRepository.deleteByEventId(eventId);

        List<Seat> seatsToSave = new ArrayList<>();

        int seatsPerRow = request.getSeatsPerRow() != null ? request.getSeatsPerRow() : 10;
        int vipRows = request.getVipRows() != null ? request.getVipRows() : 2;
        int premiumRows = request.getPremiumRows() != null ? request.getPremiumRows() : 3;
        int regularRows = request.getRegularRows() != null ? request.getRegularRows() : 5;

        BigDecimal vipPrice = request.getVipPrice() != null ? request.getVipPrice() : new BigDecimal("150.00");
        BigDecimal premiumPrice = request.getPremiumPrice() != null ? request.getPremiumPrice() : new BigDecimal("100.00");
        BigDecimal regularPrice = request.getRegularPrice() != null ? request.getRegularPrice() : new BigDecimal("50.00");

        char currentRowChar = 'A';

        // VIP Section
        for (int r = 0; r < vipRows; r++) {
            String rowName = String.valueOf(currentRowChar++);
            for (int s = 1; s <= seatsPerRow; s++) {
                seatsToSave.add(createSeat(event, "VIP", rowName, s, vipPrice));
            }
        }

        // Premium Section
        for (int r = 0; r < premiumRows; r++) {
            String rowName = String.valueOf(currentRowChar++);
            for (int s = 1; s <= seatsPerRow; s++) {
                seatsToSave.add(createSeat(event, "PREMIUM", rowName, s, premiumPrice));
            }
        }

        // Regular Section
        for (int r = 0; r < regularRows; r++) {
            String rowName = String.valueOf(currentRowChar++);
            for (int s = 1; s <= seatsPerRow; s++) {
                seatsToSave.add(createSeat(event, "REGULAR", rowName, s, regularPrice));
            }
        }

        List<Seat> savedSeats = seatRepository.saveAll(seatsToSave);

        // Sync total and available seats count on event
        event.setTotalSeats(savedSeats.size());
        event.setAvailableSeats(savedSeats.size());
        eventRepository.save(event);

        return savedSeats.stream().map(this::mapToSeatResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<SeatResponse> autoGenerateSeatsIfEmpty(Long eventId) {
        SeatGenerationRequest defaultRequest = SeatGenerationRequest.builder()
                .vipRows(2)
                .premiumRows(3)
                .regularRows(5)
                .seatsPerRow(10)
                .vipPrice(new BigDecimal("150.00"))
                .premiumPrice(new BigDecimal("100.00"))
                .regularPrice(new BigDecimal("50.00"))
                .build();
        return generateSeatsForEvent(eventId, defaultRequest);
    }

    private Seat createSeat(Event event, String sectionName, String rowName, int seatNumber, BigDecimal price) {
        return Seat.builder()
                .event(event)
                .sectionName(sectionName)
                .rowName(rowName)
                .seatNumber(seatNumber)
                .seatCode(rowName + "-" + seatNumber)
                .price(price)
                .status(SeatStatus.AVAILABLE)
                .build();
    }

    private SeatResponse mapToSeatResponse(Seat seat) {
        return SeatResponse.builder()
                .id(seat.getId())
                .eventId(seat.getEvent().getId())
                .sectionName(seat.getSectionName())
                .rowName(seat.getRowName())
                .seatNumber(seat.getSeatNumber())
                .seatCode(seat.getSeatCode())
                .price(seat.getPrice())
                .status(seat.getStatus())
                .build();
    }
}
