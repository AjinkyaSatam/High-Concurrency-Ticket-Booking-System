package com.ticketbooking.backend.service.impl;

import com.ticketbooking.backend.dto.EventRequest;
import com.ticketbooking.backend.dto.EventResponse;
import com.ticketbooking.backend.dto.VenueResponse;
import com.ticketbooking.backend.entity.Event;
import com.ticketbooking.backend.entity.EventStatus;
import com.ticketbooking.backend.entity.Venue;
import com.ticketbooking.backend.repository.EventRepository;
import com.ticketbooking.backend.repository.VenueRepository;
import com.ticketbooking.backend.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;

    @Override
    @Transactional
    public EventResponse createEvent(EventRequest request) {
        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new RuntimeException("Venue not found with id: " + request.getVenueId()));

        EventStatus status = request.getStatus() != null ? request.getStatus() : EventStatus.ON_SALE;

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .eventDate(request.getEventDate())
                .status(status)
                .bannerUrl(request.getBannerUrl())
                .venue(venue)
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .build();

        Event saved = eventRepository.save(event);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> searchEvents(String category, String city, String search) {
        String cleanCategory = (category != null && !category.isBlank()) ? category.trim() : null;
        String cleanCity = (city != null && !city.isBlank()) ? city.trim() : null;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        return eventRepository.searchEvents(cleanCategory, cleanCity, cleanSearch).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        return mapToResponse(event);
    }

    @Override
    @Transactional
    public EventResponse updateEvent(Long id, EventRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new RuntimeException("Venue not found with id: " + request.getVenueId()));

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setEventDate(request.getEventDate());
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }
        event.setBannerUrl(request.getBannerUrl());
        event.setVenue(venue);
        event.setTotalSeats(request.getTotalSeats());

        Event updated = eventRepository.save(event);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new RuntimeException("Event not found with id: " + id);
        }
        eventRepository.deleteById(id);
    }

    private EventResponse mapToResponse(Event event) {
        Venue v = event.getVenue();
        VenueResponse venueResp = VenueResponse.builder()
                .id(v.getId())
                .name(v.getName())
                .city(v.getCity())
                .address(v.getAddress())
                .capacity(v.getCapacity())
                .description(v.getDescription())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();

        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategory())
                .eventDate(event.getEventDate())
                .status(event.getStatus())
                .bannerUrl(event.getBannerUrl())
                .venue(venueResp)
                .totalSeats(event.getTotalSeats())
                .availableSeats(event.getAvailableSeats())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
