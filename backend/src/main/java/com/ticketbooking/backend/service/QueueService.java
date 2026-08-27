package com.ticketbooking.backend.service;

import com.ticketbooking.backend.dto.QueueStatusResponse;

import java.util.Map;

public interface QueueService {
    QueueStatusResponse joinQueue(Long eventId, Long userId);
    QueueStatusResponse getQueueStatus(Long eventId, String queueToken);
    int admitNextBatch(Long eventId, int batchSize);
    boolean isUserAdmitted(Long eventId, Long userId);
    Map<String, Object> getQueueStats(Long eventId);
}
