package com.ticketbooking.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeatGenerationRequest {

    @Min(value = 1, message = "VIP rows must be at least 1")
    private Integer vipRows;

    @Min(value = 1, message = "Premium rows must be at least 1")
    private Integer premiumRows;

    @Min(value = 1, message = "Regular rows must be at least 1")
    private Integer regularRows;

    @NotNull(message = "Seats per row is required")
    @Min(value = 1, message = "Seats per row must be at least 1")
    private Integer seatsPerRow;

    @NotNull(message = "VIP price is required")
    @DecimalMin(value = "0.0", message = "Price must be non-negative")
    private BigDecimal vipPrice;

    @NotNull(message = "Premium price is required")
    @DecimalMin(value = "0.0", message = "Price must be non-negative")
    private BigDecimal premiumPrice;

    @NotNull(message = "Regular price is required")
    @DecimalMin(value = "0.0", message = "Price must be non-negative")
    private BigDecimal regularPrice;
}
