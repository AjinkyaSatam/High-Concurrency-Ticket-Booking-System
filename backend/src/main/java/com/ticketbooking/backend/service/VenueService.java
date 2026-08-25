package com.ticketbooking.backend.service;

import com.ticketbooking.backend.dto.VenueRequest;
import com.ticketbooking.backend.dto.VenueResponse;

import java.util.List;

public interface VenueService {
    VenueResponse createVenue(VenueRequest request);
    List<VenueResponse> getAllVenues();
    VenueResponse getVenueById(Long id);
    VenueResponse updateVenue(Long id, VenueRequest request);
    void deleteVenue(Long id);
}
