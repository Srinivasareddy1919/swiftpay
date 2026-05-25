package com.swiftpay.ledger.service;

import com.swiftpay.ledger.event.PaymentInitiatedEvent;

/**
 * Application service port for the ledger (DIP).
 * Implementations are responsible for the atomic debit/credit workflow.
 */
public interface LedgerService {

    /**
     * Apply a payment to the ledger atomically.
     *
     * @return {@code true} when the transfer was posted (or was already posted);
     *         {@code false} when the sender has insufficient funds.
     */
    boolean process(PaymentInitiatedEvent event);
}
