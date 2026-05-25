package com.swiftpay.gateway.api;

import java.math.BigDecimal;

public record PaymentResponse(
        String transactionId,
        String senderId,
        String receiverId,
        BigDecimal amount,
        String currency,
        String status
) {}
