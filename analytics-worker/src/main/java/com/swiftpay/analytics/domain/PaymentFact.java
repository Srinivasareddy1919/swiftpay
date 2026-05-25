package com.swiftpay.analytics.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_facts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentFact {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false) private UUID transactionId;
    @Column(name = "sender_id",   nullable = false)    private String senderId;
    @Column(name = "receiver_id", nullable = false)    private String receiverId;
    @Column(nullable = false)                          private BigDecimal amount;
    @Column(nullable = false, length = 3)              private String currency;
    @Column(nullable = false, length = 16)             private String status;
    @Column(name = "occurred_at", nullable = false)    private OffsetDateTime occurredAt;
}
