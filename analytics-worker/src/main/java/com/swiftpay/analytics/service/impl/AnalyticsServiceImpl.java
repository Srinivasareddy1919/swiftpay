package com.swiftpay.analytics.service.impl;

import com.swiftpay.analytics.domain.PaymentFact;
import com.swiftpay.analytics.domain.PaymentFactRepository;
import com.swiftpay.analytics.event.PaymentCompletedEvent;
import com.swiftpay.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final PaymentFactRepository repository;

    @Override
    @Transactional
    public void record(PaymentCompletedEvent event) {
        repository.save(PaymentFact.builder()
                .transactionId(UUID.fromString(event.transactionId()))
                .senderId(event.senderId())
                .receiverId(event.receiverId())
                .amount(event.amount())
                .currency(event.currency())
                .status(event.status())
                .occurredAt(OffsetDateTime.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> summary() {
        return Map.of(
                "total",     repository.count(),
                "completed", repository.countByStatus("COMPLETED"),
                "failed",    repository.countByStatus("FAILED")
        );
    }
}
