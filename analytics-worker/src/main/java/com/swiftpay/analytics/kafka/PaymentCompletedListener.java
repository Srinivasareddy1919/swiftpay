package com.swiftpay.analytics.kafka;

import com.swiftpay.analytics.event.PaymentCompletedEvent;
import com.swiftpay.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedListener {

    private final AnalyticsService analyticsService;

    @KafkaListener(topics = "${swiftpay.topics.payment-completed}")
    public void onMessage(PaymentCompletedEvent event) {
        log.debug("Recording analytics fact for tx={}", event.transactionId());
        analyticsService.record(event);
    }
}
