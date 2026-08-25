package com.ticketbooking.backend.controller;

import com.ticketbooking.backend.dto.SeatGenerationRequest;
import com.ticketbooking.backend.dto.SeatResponse;
import com.ticketbooking.backend.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events/{eventId}/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping
    public ResponseEntity<List<SeatResponse>> getSeats(@PathVariable Long eventId) {
        return ResponseEntity.ok(seatService.getSeatsByEventId(eventId));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<SeatResponse>> generateSeats(
            @PathVariable Long eventId,
            @Valid @RequestBody SeatGenerationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seatService.generateSeatsForEvent(eventId, request));
    }
}
