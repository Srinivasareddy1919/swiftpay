package com.swiftpay.gateway.service;

/**
 * Idempotency-key reservation port (DIP).
 * Implementations decide the backing store (Redis today, DB tomorrow).
 */
public interface IdempotencyService {

    /**
     * @return {@code true} when the key was reserved for the first time;
     *         {@code false} when the same key has already been seen.
     */
    boolean reserve(String transactionId);
}
