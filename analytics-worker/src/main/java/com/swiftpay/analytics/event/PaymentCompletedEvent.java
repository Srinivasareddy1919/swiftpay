package com.swiftpay.analytics.event;

import java.math.BigDecimal;

public record PaymentCompletedEvent(
        String transactionId,
        String senderId,
        String receiverId,
        BigDecimal amount,
        String currency,
        String status,
        String reason
) {}
