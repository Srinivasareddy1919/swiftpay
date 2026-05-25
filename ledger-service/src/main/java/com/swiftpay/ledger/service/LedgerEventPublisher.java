package com.swiftpay.ledger.service;

import com.swiftpay.ledger.event.PaymentInitiatedEvent;

/** Outbound event-publisher port for ledger outcomes (DIP). */
public interface LedgerEventPublisher {
    void publishCompleted(PaymentInitiatedEvent source);
    void publishFailed(PaymentInitiatedEvent source, String reason);
}
