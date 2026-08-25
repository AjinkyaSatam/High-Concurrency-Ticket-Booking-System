package com.ticketbooking.backend.repository;

import com.ticketbooking.backend.entity.Event;
import com.ticketbooking.backend.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    
    List<Event> findByCategoryIgnoreCase(String category);
    
    List<Event> findByStatus(EventStatus status);

    @Query("SELECT e FROM Event e WHERE " +
           "(:category IS NULL OR LOWER(e.category) = LOWER(:category)) AND " +
           "(:city IS NULL OR LOWER(e.venue.city) = LOWER(:city)) AND " +
           "(:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Event> searchEvents(@Param("category") String category,
                             @Param("city") String city,
                             @Param("search") String search);
}
