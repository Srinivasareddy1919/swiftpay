package com.swiftpay.analytics.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentFactRepository extends JpaRepository<PaymentFact, Long> {
    long countByStatus(String status);
}
