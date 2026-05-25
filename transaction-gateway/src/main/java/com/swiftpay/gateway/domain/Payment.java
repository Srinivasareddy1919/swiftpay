package com.swiftpay.gateway.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "sender_id", nullable = false)   private String senderId;
    @Column(name = "receiver_id", nullable = false) private String receiverId;
    @Column(nullable = false) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus status;

    @Column(name = "created_at") private OffsetDateTime createdAt;
    @Column(name = "updated_at") private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }
    @PreUpdate
    void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
