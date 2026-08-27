package com.ticketbooking.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatusResponse {
    private Long eventId;
    private String queueToken;
    private Long userId;
    private long position;
    private long totalWaiting;
    private long estimatedWaitSeconds;
    private String status; // WAITING, ADMITTED, EXPIRED
    private String admissionCode;
    private LocalDateTime joinedAt;
    private LocalDateTime admittedAt;
}
