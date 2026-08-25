package com.ticketbooking.backend.dto;

import com.ticketbooking.backend.entity.SeatHoldStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeatHoldResponse {
    private Long id;
    private String holdReference;
    private Long eventId;
    private String eventTitle;
    private SeatHoldStatus status;
    private OffsetDateTime expiresAt;
    private Long remainingSeconds;
    private BigDecimal totalAmount;
    private List<SeatResponse> seats;
}
