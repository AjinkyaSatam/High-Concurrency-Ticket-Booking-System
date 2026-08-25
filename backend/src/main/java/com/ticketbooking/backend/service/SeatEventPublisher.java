package com.ticketbooking.backend.service;

import com.ticketbooking.backend.dto.SeatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishSeatUpdates(Long eventId, List<SeatResponse> updatedSeats) {
        if (updatedSeats == null || updatedSeats.isEmpty()) return;

        String destination = "/topic/events/" + eventId + "/seats";
        try {
            messagingTemplate.convertAndSend(destination, updatedSeats);
            log.info("Broadcasted {} updated seat(s) to WebSocket topic: {}", updatedSeats.size(), destination);
        } catch (Exception e) {
            log.error("Failed to broadcast seat updates via WebSocket", e);
        }
    }
}
