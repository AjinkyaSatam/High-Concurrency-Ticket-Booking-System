package com.ticketbooking.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueResponse {
    private Long id;
    private String name;
    private String city;
    private String address;
    private Integer capacity;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
