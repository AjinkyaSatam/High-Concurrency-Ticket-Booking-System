package com.ticketbooking.backend.service.impl;

import com.ticketbooking.backend.dto.QueueStatusResponse;
import com.ticketbooking.backend.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueServiceImpl implements QueueService {

    private final RedissonClient redissonClient;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String QUEUE_KEY_PREFIX = "ticketbooking:queue:";
    private static final String TOKENS_KEY_PREFIX = "ticketbooking:queue_tokens:";
    private static final String REVERSE_TOKENS_KEY_PREFIX = "ticketbooking:queue_token_to_user:";
    private static final String ADMITTED_KEY_PREFIX = "ticketbooking:queue_admitted:";

    @Override
    public QueueStatusResponse joinQueue(Long eventId, Long userId) {
        String queueKey = QUEUE_KEY_PREFIX + eventId;
        String tokensKey = TOKENS_KEY_PREFIX + eventId;
        String reverseTokensKey = REVERSE_TOKENS_KEY_PREFIX + eventId;
        String admittedKey = ADMITTED_KEY_PREFIX + eventId;

        RScoredSortedSet<String> queue = redissonClient.getScoredSortedSet(queueKey);
        RMap<String, String> tokens = redissonClient.getMap(tokensKey);
        RMap<String, String> tokenToUser = redissonClient.getMap(reverseTokensKey);
        RMap<Long, String> admitted = redissonClient.getMap(admittedKey);

        // Check if user is already admitted
        if (admitted.containsKey(userId)) {
            String admissionCode = admitted.get(userId);
            return QueueStatusResponse.builder()
                    .eventId(eventId)
                    .userId(userId)
                    .queueToken("ADMITTED_" + userId)
                    .position(0)
                    .totalWaiting(queue.size())
                    .estimatedWaitSeconds(0)
                    .status("ADMITTED")
                    .admissionCode(admissionCode)
                    .joinedAt(LocalDateTime.now())
                    .admittedAt(LocalDateTime.now())
                    .build();
        }

        // Check if user is already in queue
        Double existingScore = queue.getScore(String.valueOf(userId));
        String token;
        long score;

        if (existingScore != null) {
            score = existingScore.longValue();
            token = tokens.get(String.valueOf(userId));
            if (token == null) {
                token = "QT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                tokens.put(String.valueOf(userId), token);
                tokenToUser.put(token, String.valueOf(userId));
            }
        } else {
            score = Instant.now().toEpochMilli();
            token = "QT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            queue.add(score, String.valueOf(userId));
            tokens.put(String.valueOf(userId), token);
            tokenToUser.put(token, String.valueOf(userId));
        }

        Integer rank = queue.rank(String.valueOf(userId));
        long position = (rank != null) ? rank + 1 : 1;
        long estimatedWait = position * 5L; // 5 seconds per position batch

        QueueStatusResponse response = QueueStatusResponse.builder()
                .eventId(eventId)
                .queueToken(token)
                .userId(userId)
                .position(position)
                .totalWaiting(queue.size())
                .estimatedWaitSeconds(estimatedWait)
                .status("WAITING")
                .joinedAt(LocalDateTime.now())
                .build();

        log.info("User {} joined queue for event {}. Position: {}, Token: {}", userId, eventId, position, token);
        return response;
    }

    @Override
    public QueueStatusResponse getQueueStatus(Long eventId, String queueToken) {
        String queueKey = QUEUE_KEY_PREFIX + eventId;
        String reverseTokensKey = REVERSE_TOKENS_KEY_PREFIX + eventId;
        String admittedKey = ADMITTED_KEY_PREFIX + eventId;

        RScoredSortedSet<String> queue = redissonClient.getScoredSortedSet(queueKey);
        RMap<String, String> tokenToUser = redissonClient.getMap(reverseTokensKey);
        RMap<Long, String> admitted = redissonClient.getMap(admittedKey);

        // Fast O(1) user lookup by token
        Long userId = null;
        String userIdStr = tokenToUser.get(queueToken);
        if (userIdStr != null) {
            try {
                userId = Long.parseLong(userIdStr);
            } catch (NumberFormatException ignored) {}
        } else if (queueToken != null && queueToken.startsWith("ADMITTED_")) {
            try {
                userId = Long.parseLong(queueToken.replace("ADMITTED_", ""));
            } catch (NumberFormatException ignored) {}
        }

        if (userId == null) {
            return QueueStatusResponse.builder()
                    .eventId(eventId)
                    .queueToken(queueToken)
                    .status("EXPIRED")
                    .position(-1)
                    .build();
        }

        if (admitted.containsKey(userId)) {
            return QueueStatusResponse.builder()
                    .eventId(eventId)
                    .userId(userId)
                    .queueToken(queueToken)
                    .position(0)
                    .totalWaiting(queue.size())
                    .estimatedWaitSeconds(0)
                    .status("ADMITTED")
                    .admissionCode(admitted.get(userId))
                    .admittedAt(LocalDateTime.now())
                    .build();
        }

        Integer rank = queue.rank(String.valueOf(userId));
        if (rank == null) {
            return QueueStatusResponse.builder()
                    .eventId(eventId)
                    .queueToken(queueToken)
                    .status("EXPIRED")
                    .position(-1)
                    .build();
        }

        long position = rank + 1;
        long estimatedWait = position * 5L;

        return QueueStatusResponse.builder()
                .eventId(eventId)
                .userId(userId)
                .queueToken(queueToken)
                .position(position)
                .totalWaiting(queue.size())
                .estimatedWaitSeconds(estimatedWait)
                .status("WAITING")
                .build();
    }

    @Override
    public int admitNextBatch(Long eventId, int batchSize) {
        String queueKey = QUEUE_KEY_PREFIX + eventId;
        String tokensKey = TOKENS_KEY_PREFIX + eventId;
        String reverseTokensKey = REVERSE_TOKENS_KEY_PREFIX + eventId;
        String admittedKey = ADMITTED_KEY_PREFIX + eventId;

        RScoredSortedSet<String> queue = redissonClient.getScoredSortedSet(queueKey);
        RMap<String, String> tokens = redissonClient.getMap(tokensKey);
        RMap<String, String> tokenToUser = redissonClient.getMap(reverseTokensKey);
        RMap<Long, String> admitted = redissonClient.getMap(admittedKey);

        int admittedCount = 0;
        while (admittedCount < batchSize && !queue.isEmpty()) {
            String userIdStr = queue.pollFirst();
            if (userIdStr != null) {
                Long userId = Long.parseLong(userIdStr);
                String admissionCode = "PASS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                admitted.put(userId, admissionCode);

                String token = tokens.get(userIdStr);
                if (token != null) {
                    tokenToUser.remove(token);
                }
                tokens.remove(userIdStr);
                admittedCount++;

                // Notify user via WebSocket
                QueueStatusResponse status = QueueStatusResponse.builder()
                        .eventId(eventId)
                        .userId(userId)
                        .position(0)
                        .status("ADMITTED")
                        .admissionCode(admissionCode)
                        .admittedAt(LocalDateTime.now())
                        .build();

                try {
                    messagingTemplate.convertAndSend("/topic/queue/" + eventId + "/" + userId, status);
                } catch (Exception e) {
                    log.error("Failed to send queue admission WebSocket update to user {}", userId, e);
                }
            }
        }

        // Notify remaining waiting users of updated positions
        notifyQueuePositions(eventId);

        log.info("Admitted batch of {} users for event {}", admittedCount, eventId);
        return admittedCount;
    }

    @Override
    public boolean isUserAdmitted(Long eventId, Long userId) {
        String admittedKey = ADMITTED_KEY_PREFIX + eventId;
        RMap<Long, String> admitted = redissonClient.getMap(admittedKey);
        return admitted.containsKey(userId);
    }

    @Override
    public Map<String, Object> getQueueStats(Long eventId) {
        String queueKey = QUEUE_KEY_PREFIX + eventId;
        String admittedKey = ADMITTED_KEY_PREFIX + eventId;

        RScoredSortedSet<String> queue = redissonClient.getScoredSortedSet(queueKey);
        RMap<Long, String> admitted = redissonClient.getMap(admittedKey);

        Map<String, Object> stats = new HashMap<>();
        stats.put("eventId", eventId);
        stats.put("totalWaiting", queue.size());
        stats.put("totalAdmitted", admitted.size());
        stats.put("queueActive", true);
        return stats;
    }

    private void notifyQueuePositions(Long eventId) {
        String queueKey = QUEUE_KEY_PREFIX + eventId;
        String tokensKey = TOKENS_KEY_PREFIX + eventId;
        RScoredSortedSet<String> queue = redissonClient.getScoredSortedSet(queueKey);
        RMap<String, String> tokens = redissonClient.getMap(tokensKey);

        long index = 1;
        for (String userIdStr : queue) {
            Long userId = Long.parseLong(userIdStr);
            String token = tokens.get(userIdStr);
            QueueStatusResponse status = QueueStatusResponse.builder()
                    .eventId(eventId)
                    .userId(userId)
                    .queueToken(token)
                    .position(index)
                    .totalWaiting(queue.size())
                    .estimatedWaitSeconds(index * 5L)
                    .status("WAITING")
                    .build();

            try {
                messagingTemplate.convertAndSend("/topic/queue/" + eventId + "/" + userId, status);
            } catch (Exception e) {
                log.warn("Could not notify position for user {}", userId);
            }
            index++;
        }
    }
}
