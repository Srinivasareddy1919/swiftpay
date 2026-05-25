package com.swiftpay.ledger.event;
import java.math.BigDecimal;
public record PaymentInitiatedEvent(String transactionId, String senderId, String receiverId,
                                    BigDecimal amount, String currency) {}
