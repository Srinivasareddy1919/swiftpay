package com.swiftpay.ledger.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "ledger_entries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "transaction_id", nullable = false) private UUID transactionId;
    @Column(name = "user_id", nullable = false) private String userId;
    @Column(nullable = false, length = 8) private String direction; // DEBIT | CREDIT
    @Column(nullable = false) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "created_at") private OffsetDateTime createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = OffsetDateTime.now(); }
}
