package com.ticketbooking.backend.controller;

import com.ticketbooking.backend.dto.QueueStatusResponse;
import com.ticketbooking.backend.entity.User;
import com.ticketbooking.backend.repository.UserRepository;
import com.ticketbooking.backend.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;
    private final UserRepository userRepository;

    @PostMapping("/join")
    public ResponseEntity<QueueStatusResponse> joinQueue(
            Principal principal,
            @RequestParam Long eventId) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found: " + principal.getName()));
        return ResponseEntity.ok(queueService.joinQueue(eventId, user.getId()));
    }

    @GetMapping("/status")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(
            @RequestParam Long eventId,
            @RequestParam String queueToken) {
        return ResponseEntity.ok(queueService.getQueueStatus(eventId, queueToken));
    }

    @PostMapping("/admin/drain")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> admitBatch(
            @RequestParam Long eventId,
            @RequestParam(defaultValue = "10") int batchSize) {
        int admittedCount = queueService.admitNextBatch(eventId, batchSize);
        return ResponseEntity.ok(Map.of(
                "eventId", eventId,
                "admittedCount", admittedCount,
                "status", "SUCCESS"
        ));
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getQueueStats(@RequestParam Long eventId) {
        return ResponseEntity.ok(queueService.getQueueStats(eventId));
    }
}
