package com.swiftpay.gateway.service;

import com.swiftpay.gateway.api.PaymentRequest;
import com.swiftpay.gateway.api.PaymentResponse;
import com.swiftpay.gateway.domain.Payment;
import com.swiftpay.gateway.domain.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * SRP: pure DTO <-> entity translation, free of side effects.
 */
@Component
public class PaymentMapper {

    public Payment toEntity(PaymentRequest request) {
        return Payment.builder()
                .transactionId(parseUuid(request.transactionId()))
                .senderId(request.senderId())
                .receiverId(request.receiverId())
                .amount(request.amount())
                .currency(request.currency())
                .status(PaymentStatus.PENDING)
                .build();
    }

    public PaymentResponse toResponse(Payment entity) {
        return new PaymentResponse(
                entity.getTransactionId().toString(),
                entity.getSenderId(),
                entity.getReceiverId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStatus().name()
        );
    }

    public UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("transactionId must be a UUID");
        }
    }
}
