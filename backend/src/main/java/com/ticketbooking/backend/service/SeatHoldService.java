package com.ticketbooking.backend.service;

import com.ticketbooking.backend.dto.BookingResponse;
import com.ticketbooking.backend.dto.SeatHoldRequest;
import com.ticketbooking.backend.dto.SeatHoldResponse;

public interface SeatHoldService {
    SeatHoldResponse createHold(String userEmail, SeatHoldRequest request);
    SeatHoldResponse getHoldByReference(String userEmail, String holdReference);
    BookingResponse confirmHoldToBooking(String userEmail, String holdReference);
    void releaseExpiredHolds();
}
