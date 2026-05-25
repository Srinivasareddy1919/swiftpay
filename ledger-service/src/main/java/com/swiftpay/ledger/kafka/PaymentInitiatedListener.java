package com.swiftpay.ledger.kafka;

import com.swiftpay.ledger.event.PaymentInitiatedEvent;
import com.swiftpay.ledger.service.LedgerEventPublisher;
import com.swiftpay.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Thin Kafka adapter — only translates protocol concerns into a service call,
 * then delegates publishing to {@link LedgerEventPublisher}. No business logic
 * lives here (SRP / Hexagonal ports & adapters).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentInitiatedListener {

    private static final String REASON_INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";

    private final LedgerService ledgerService;
    private final LedgerEventPublisher eventPublisher;

    @KafkaListener(topics = "${swiftpay.topics.payment-initiated}",
                   containerFactory = "kafkaListenerContainerFactory")
    @Retryable(retryFor = TransientDataAccessException.class,
               maxAttempts = 5,
               backoff = @Backoff(delay = 500, multiplier = 2.0, maxDelay = 8000))
    public void onMessage(PaymentInitiatedEvent event, Acknowledgment ack) {
        log.info("Processing PaymentInitiated tx={}", event.transactionId());

        if (ledgerService.process(event)) {
            eventPublisher.publishCompleted(event);
        } else {
            eventPublisher.publishFailed(event, REASON_INSUFFICIENT_FUNDS);
        }
        ack.acknowledge();
    }
}
