package com.ticketbooking.backend.dto;

import com.ticketbooking.backend.entity.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeatResponse {
    private Long id;
    private Long eventId;
    private String sectionName;
    private String rowName;
    private Integer seatNumber;
    private String seatCode;
    private BigDecimal price;
    private SeatStatus status;
}
