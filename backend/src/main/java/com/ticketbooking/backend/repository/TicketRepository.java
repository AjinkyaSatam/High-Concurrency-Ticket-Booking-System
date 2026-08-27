package com.ticketbooking.backend.repository;

import com.ticketbooking.backend.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByBookingId(Long bookingId);
    java.util.Optional<Ticket> findByTicketCode(String ticketCode);
}
