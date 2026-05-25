package com.swiftpay.ledger.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity @Table(name = "accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Account {
    @Id @Column(name = "user_id") private String userId;
    @Column(nullable = false) private BigDecimal balance;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "updated_at") private OffsetDateTime updatedAt;
}
