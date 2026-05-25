package com.swiftpay.ledger.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByUserIdOrderByCreatedAtDesc(String userId);
    boolean existsByTransactionId(UUID transactionId);
}
