package com.ticketbooking.backend.service;

import com.ticketbooking.backend.dto.BookingRequest;
import com.ticketbooking.backend.dto.BookingResponse;

import java.util.List;

public interface BookingService {
    BookingResponse createBooking(String userEmail, BookingRequest request);
    List<BookingResponse> getUserBookings(String userEmail);
    BookingResponse getBookingById(String userEmail, Long bookingId);
    BookingResponse cancelBooking(String userEmail, Long bookingId);
}
