package com.ticketbooking.backend.repository;

import com.ticketbooking.backend.entity.Seat;
import com.ticketbooking.backend.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByEventIdOrderBySectionNameAscRowNameAscSeatNumberAsc(Long eventId);
    List<Seat> findByEventIdAndIdIn(Long eventId, List<Long> seatIds);
    long countByEventIdAndStatus(Long eventId, SeatStatus status);
    void deleteByEventId(Long eventId);
}
