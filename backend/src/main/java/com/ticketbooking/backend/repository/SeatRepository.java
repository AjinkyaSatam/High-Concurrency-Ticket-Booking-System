package com.ticketbooking.backend.repository;

import com.ticketbooking.backend.entity.Seat;
import com.ticketbooking.backend.entity.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByEventIdOrderBySectionNameAscRowNameAscSeatNumberAsc(Long eventId);

    List<Seat> findByEventIdAndIdIn(Long eventId, List<Long> seatIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.event.id = :eventId AND s.id IN :seatIds")
    List<Seat> findByEventIdAndIdInWithLock(@Param("eventId") Long eventId, @Param("seatIds") List<Long> seatIds);

    long countByEventIdAndStatus(Long eventId, SeatStatus status);

    void deleteByEventId(Long eventId);
}
