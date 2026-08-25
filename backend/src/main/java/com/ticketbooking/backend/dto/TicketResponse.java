package com.ticketbooking.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketResponse {
    private Long id;
    private String ticketCode;
    private Long seatId;
    private String sectionName;
    private String rowName;
    private Integer seatNumber;
    private String seatCode;
    private BigDecimal price;
}
