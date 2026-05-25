package com.swiftpay.gateway.service;

import com.swiftpay.gateway.domain.Payment;

/**
 * Outbound event-publisher port (DIP). The service layer depends on this
 * abstraction; the Kafka implementation lives in {@code service.impl}.
 */
public interface PaymentEventPublisher {
    void publishInitiated(Payment payment);
}
