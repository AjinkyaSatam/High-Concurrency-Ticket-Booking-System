package com.ticketbooking.backend.repository;

import com.ticketbooking.backend.entity.SeatHold;
import com.ticketbooking.backend.entity.SeatHoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {
    Optional<SeatHold> findByHoldReference(String holdReference);
    Optional<SeatHold> findByHoldReferenceAndUserId(String holdReference, Long userId);
    List<SeatHold> findByStatusAndExpiresAtBefore(SeatHoldStatus status, OffsetDateTime now);
}
