package com.ticketbooking.backend.service;

import com.ticketbooking.backend.dto.EventRequest;
import com.ticketbooking.backend.dto.EventResponse;

import java.util.List;

public interface EventService {
    EventResponse createEvent(EventRequest request);
    List<EventResponse> getAllEvents();
    List<EventResponse> searchEvents(String category, String city, String search);
    EventResponse getEventById(Long id);
    EventResponse updateEvent(Long id, EventRequest request);
    void deleteEvent(Long id);
}
