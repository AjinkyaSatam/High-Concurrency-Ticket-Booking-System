package com.ticketbooking.backend.service.impl;

import com.ticketbooking.backend.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockServiceImpl implements DistributedLockService {

    private final RedissonClient redissonClient;

    @Override
    public <T> T executeWithSeatLocks(List<Long> seatIds, long waitTimeSec, long leaseTimeSec, Supplier<T> task) {
        if (seatIds == null || seatIds.isEmpty()) {
            return task.get();
        }

        // Sort seat IDs to maintain consistent lock acquisition order across all threads (prevents deadlock)
        List<Long> sortedSeatIds = new ArrayList<>(seatIds);
        Collections.sort(sortedSeatIds);

        List<RLock> lockList = new ArrayList<>();
        for (Long seatId : sortedSeatIds) {
            lockList.add(redissonClient.getLock("lock:seat:" + seatId));
        }

        RLock combinedLock;
        if (lockList.size() == 1) {
            combinedLock = lockList.get(0);
        } else {
            combinedLock = redissonClient.getMultiLock(lockList.toArray(new RLock[0]));
        }

        boolean acquired = false;
        try {
            log.info("Attempting to acquire distributed lock for seats: {}", sortedSeatIds);
            acquired = combinedLock.tryLock(waitTimeSec, leaseTimeSec, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Failed to acquire distributed lock for seats {} within {}s", sortedSeatIds, waitTimeSec);
                throw new IllegalStateException("High concurrency lock collision: Seats " + sortedSeatIds + " are currently locked by another transaction");
            }

            log.info("Successfully acquired distributed lock for seats: {}", sortedSeatIds);
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting for seat locks", e);
        } finally {
            if (acquired && combinedLock.isHeldByCurrentThread()) {
                combinedLock.unlock();
                log.info("Released distributed lock for seats: {}", sortedSeatIds);
            }
        }
    }
}
