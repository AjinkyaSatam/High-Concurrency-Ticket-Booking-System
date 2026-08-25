package com.ticketbooking.backend.dto;

import com.ticketbooking.backend.entity.BookingStatus;
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
public class BookingResponse {
    private Long id;
    private String bookingReference;
    private Long userId;
    private String userEmail;
    private String userName;
    private Long eventId;
    private String eventTitle;
    private String venueName;
    private OffsetDateTime eventDate;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private OffsetDateTime bookingTime;
    private List<TicketResponse> tickets;
}
