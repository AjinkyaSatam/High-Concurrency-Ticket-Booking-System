package com.ticketbooking.backend.dto;

import com.ticketbooking.backend.entity.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private OffsetDateTime eventDate;
    private EventStatus status;
    private String bannerUrl;
    private VenueResponse venue;
    private Integer totalSeats;
    private Integer availableSeats;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
