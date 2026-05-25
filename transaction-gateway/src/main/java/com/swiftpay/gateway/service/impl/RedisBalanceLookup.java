package com.swiftpay.gateway.service.impl;

import com.swiftpay.gateway.service.BalanceLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Cache-backed {@link BalanceLookup}; falls back to a demo default when unset. */
@Component
@RequiredArgsConstructor
public class RedisBalanceLookup implements BalanceLookup {

    private static final String KEY_PREFIX = "swiftpay:balance:";
    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("10000.0000");

    private final StringRedisTemplate redis;

    @Override
    public BigDecimal balanceOf(String userId) {
        String cached = redis.opsForValue().get(KEY_PREFIX + userId);
        return cached == null ? DEFAULT_BALANCE : new BigDecimal(cached);
    }
}
