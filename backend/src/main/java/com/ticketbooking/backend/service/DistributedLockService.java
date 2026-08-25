package com.ticketbooking.backend.service;

import java.util.List;
import java.util.function.Supplier;

public interface DistributedLockService {
    <T> T executeWithSeatLocks(List<Long> seatIds, long waitTimeSec, long leaseTimeSec, Supplier<T> task);
}
