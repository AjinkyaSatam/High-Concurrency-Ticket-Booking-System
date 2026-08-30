package com.ticketbooking.backend.config;

import org.mockito.Mockito;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@Profile("test")
public class TestRedisConfig {

    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        RedissonClient mockClient = mock(RedissonClient.class);

        when(mockClient.getLock(anyString())).thenAnswer(invocation -> {
            String lockName = invocation.getArgument(0);
            ReentrantLock realLock = lockMap.computeIfAbsent(lockName, k -> new ReentrantLock());
            RLock rLock = mock(RLock.class);

            try {
                when(rLock.tryLock(any(Long.class), any(Long.class), any(TimeUnit.class))).thenAnswer(i -> {
                    long waitTime = i.getArgument(0);
                    TimeUnit unit = i.getArgument(2);
                    return realLock.tryLock(waitTime, unit);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            when(rLock.isHeldByCurrentThread()).thenAnswer(i -> realLock.isHeldByCurrentThread());

            Mockito.doAnswer(i -> {
                if (realLock.isHeldByCurrentThread()) {
                    realLock.unlock();
                }
                return null;
            }).when(rLock).unlock();

            return rLock;
        });

        when(mockClient.getMultiLock(any())).thenAnswer(invocation -> {
            RLock[] locks = invocation.getArgument(0);
            if (locks != null && locks.length > 0) {
                return locks[0];
            }
            return mock(RLock.class);
        });

        return mockClient;
    }
}
