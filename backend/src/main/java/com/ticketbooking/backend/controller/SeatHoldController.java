package com.ticketbooking.backend.controller;

import com.ticketbooking.backend.dto.BookingResponse;
import com.ticketbooking.backend.dto.SeatHoldRequest;
import com.ticketbooking.backend.dto.SeatHoldResponse;
import com.ticketbooking.backend.service.SeatHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/holds")
@RequiredArgsConstructor
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    @PostMapping
    public ResponseEntity<SeatHoldResponse> createHold(
            Principal principal,
            @Valid @RequestBody SeatHoldRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seatHoldService.createHold(principal.getName(), request));
    }

    @GetMapping("/{holdRef}")
    public ResponseEntity<SeatHoldResponse> getHold(Principal principal, @PathVariable String holdRef) {
        return ResponseEntity.ok(seatHoldService.getHoldByReference(principal.getName(), holdRef));
    }

    @PostMapping("/{holdRef}/confirm")
    public ResponseEntity<BookingResponse> confirmHold(Principal principal, @PathVariable String holdRef) {
        return ResponseEntity.ok(seatHoldService.confirmHoldToBooking(principal.getName(), holdRef));
    }
}
