package com.swiftpay.gateway.service.impl;

import com.swiftpay.gateway.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** Redis-backed implementation of {@link IdempotencyService} using SETNX + TTL. */
@Service
@RequiredArgsConstructor
public class RedisIdempotencyService implements IdempotencyService {

    private static final String KEY_PREFIX = "swiftpay:idemp:";

    private final StringRedisTemplate redis;

    @Value("${swiftpay.idempotency.ttl-hours:24}")
    private long ttlHours;

    @Override
    public boolean reserve(String transactionId) {
        Boolean firstSeen = redis.opsForValue()
                .setIfAbsent(KEY_PREFIX + transactionId, "1", Duration.ofHours(ttlHours));
        return Boolean.TRUE.equals(firstSeen);
    }
}
