package com.ticketbooking.backend.service;

import com.ticketbooking.backend.dto.SeatGenerationRequest;
import com.ticketbooking.backend.dto.SeatResponse;

import java.util.List;

public interface SeatService {
    List<SeatResponse> getSeatsByEventId(Long eventId);
    List<SeatResponse> generateSeatsForEvent(Long eventId, SeatGenerationRequest request);
    List<SeatResponse> autoGenerateSeatsIfEmpty(Long eventId);
}
