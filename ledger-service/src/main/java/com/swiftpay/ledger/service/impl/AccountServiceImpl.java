package com.swiftpay.ledger.service.impl;

import com.swiftpay.ledger.domain.Account;
import com.swiftpay.ledger.domain.AccountRepository;
import com.swiftpay.ledger.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("10000.0000");

    private final AccountRepository accounts;

    @Override
    public Account loadOrCreate(String userId, String currency) {
        return accounts.findByUserId(userId).orElseGet(() -> accounts.save(Account.builder()
                .userId(userId)
                .balance(DEFAULT_BALANCE)
                .currency(currency)
                .updatedAt(OffsetDateTime.now())
                .build()));
    }
}
