package com.swiftpay.ledger.service.impl;

import com.swiftpay.ledger.event.PaymentCompletedEvent;
import com.swiftpay.ledger.event.PaymentInitiatedEvent;
import com.swiftpay.ledger.service.LedgerEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaLedgerEventPublisher implements LedgerEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${swiftpay.topics.payment-completed}") private String completedTopic;
    @Value("${swiftpay.topics.payment-failed}")    private String failedTopic;

    @Override
    public void publishCompleted(PaymentInitiatedEvent source) {
        kafkaTemplate.send(completedTopic, source.transactionId(),
                new PaymentCompletedEvent(source.transactionId(), source.senderId(), source.receiverId(),
                        source.amount(), source.currency(), "COMPLETED", null));
    }

    @Override
    public void publishFailed(PaymentInitiatedEvent source, String reason) {
        kafkaTemplate.send(failedTopic, source.transactionId(),
                new PaymentCompletedEvent(source.transactionId(), source.senderId(), source.receiverId(),
                        source.amount(), source.currency(), "FAILED", reason));
    }
}
