package com.parkease.parking.service.impl;

import com.parkease.parking.service.AvailabilityCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityCounterServiceImpl implements AvailabilityCounterService {

    private final StringRedisTemplate redisTemplate;

    // Key pattern: "availability:lot:{lotId}"
    private static final String KEY_PREFIX = "availability:lot:";

    // ── Init

    @Override
    public void initCounter(Long lotId, int availableSpots) {
        String key = buildKey(lotId);
        redisTemplate.opsForValue().set(key, String.valueOf(availableSpots));
        log.info("Redis counter initialized: lot={} count={}", lotId, availableSpots);
    }

    // ── Increment (spot becomes available — checkout or cancellation)

    @Override
    public void increment(Long lotId) {
        String key = buildKey(lotId);
        Long newCount = redisTemplate.opsForValue().increment(key);
        log.debug("Redis counter incremented: lot={} newCount={}", lotId, newCount);
    }

    // ── Decrement (spot gets reserved — booking created)

    @Override
    public void decrement(Long lotId) {
        String key = buildKey(lotId);
        Long current = getCurrentCount(key);

        if (current <= 0) {
            throw new IllegalStateException("No available spots in lot: " + lotId);
        }

        Long newCount = redisTemplate.opsForValue().decrement(key);
        log.debug("Redis counter decremented: lot={} newCount={}", lotId, newCount);
    }

    // ── Get count

    @Override
    public int getAvailableCount(Long lotId) {
        String key = buildKey(lotId);
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            log.warn("No Redis counter found for lot={}, returning 0", lotId);
            return 0;
        }

        return Integer.parseInt(value);
    }

    // ── Check availability

    @Override
    public boolean isAvailable(Long lotId) {
        return getAvailableCount(lotId) > 0;
    }

    // ── Delete (lot deleted or rejected)

    @Override
    public void deleteCounter(Long lotId) {
        redisTemplate.delete(buildKey(lotId));
        log.info("Redis counter deleted: lot={}", lotId);
    }

    // ── Private Helpers

    private String buildKey(Long lotId) {
        return KEY_PREFIX + lotId;
    }

    private Long getCurrentCount(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) return 0L;
        return Long.parseLong(value);
    }
}