package com.swiftpay.ledger.service.impl;

import com.swiftpay.ledger.domain.Account;
import com.swiftpay.ledger.domain.LedgerEntry;
import com.swiftpay.ledger.domain.LedgerEntryRepository;
import com.swiftpay.ledger.event.PaymentInitiatedEvent;
import com.swiftpay.ledger.service.AccountService;
import com.swiftpay.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Default {@link LedgerService}. Orchestrates the atomic debit/credit and
 * delegates account lifecycle to {@link AccountService} (SRP/DIP).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerServiceImpl implements LedgerService {

    private final AccountService accountService;
    private final LedgerEntryRepository entries;

    @Override
    @Transactional
    public boolean process(PaymentInitiatedEvent event) {
        UUID txId = UUID.fromString(event.transactionId());
        if (entries.existsByTransactionId(txId)) {
            log.info("Transaction {} already posted; skipping", txId);
            return true;
        }

        Account sender   = accountService.loadOrCreate(event.senderId(),   event.currency());
        Account receiver = accountService.loadOrCreate(event.receiverId(), event.currency());

        if (sender.getBalance().compareTo(event.amount()) < 0) {
            log.warn("Insufficient funds for {} (balance={}, amount={})",
                    sender.getUserId(), sender.getBalance(), event.amount());
            return false;
        }

        OffsetDateTime now = OffsetDateTime.now();
        sender.setBalance(sender.getBalance().subtract(event.amount()));
        sender.setUpdatedAt(now);
        receiver.setBalance(receiver.getBalance().add(event.amount()));
        receiver.setUpdatedAt(now);

        entries.save(LedgerEntry.builder()
                .transactionId(txId).userId(sender.getUserId())
                .direction("DEBIT").amount(event.amount()).currency(event.currency()).build());
        entries.save(LedgerEntry.builder()
                .transactionId(txId).userId(receiver.getUserId())
                .direction("CREDIT").amount(event.amount()).currency(event.currency()).build());
        return true;
    }
}
