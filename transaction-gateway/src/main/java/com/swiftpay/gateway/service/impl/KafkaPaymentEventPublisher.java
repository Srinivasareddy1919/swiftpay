package com.swiftpay.gateway.service.impl;

import com.swiftpay.gateway.domain.Payment;
import com.swiftpay.gateway.event.PaymentInitiatedEvent;
import com.swiftpay.gateway.service.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Kafka adapter implementing {@link PaymentEventPublisher}. */
@Component
@RequiredArgsConstructor
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${swiftpay.topics.payment-initiated}")
    private String paymentInitiatedTopic;

    @Override
    public void publishInitiated(Payment payment) {
        PaymentInitiatedEvent event = new PaymentInitiatedEvent(
                payment.getTransactionId().toString(),
                payment.getSenderId(),
                payment.getReceiverId(),
                payment.getAmount(),
                payment.getCurrency()
        );
        kafkaTemplate.send(paymentInitiatedTopic, event.transactionId(), event);
    }
}
