package com.ticketbooking.backend.controller;

import com.ticketbooking.backend.dto.BookingRequest;
import com.ticketbooking.backend.dto.BookingResponse;
import com.ticketbooking.backend.service.BookingService;
import com.ticketbooking.backend.service.ETicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final ETicketService eTicketService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            Principal principal,
            @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(principal.getName(), request));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Principal principal) {
        return ResponseEntity.ok(bookingService.getUserBookings(principal.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(principal.getName(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(principal.getName(), id));
    }

    @GetMapping("/{id}/ticket-pdf")
    public ResponseEntity<byte[]> downloadETicketPdf(Principal principal, @PathVariable Long id) {
        byte[] pdfBytes = eTicketService.generateETicketPdf(principal.getName(), id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.setContentDispositionFormData("attachment", "eticket-booking-" + id + ".html");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/verify/{ticketCode}")
    public ResponseEntity<Map<String, Object>> verifyTicket(@PathVariable String ticketCode) {
        return ResponseEntity.ok(eTicketService.verifyTicketQr(ticketCode));
    }
}
