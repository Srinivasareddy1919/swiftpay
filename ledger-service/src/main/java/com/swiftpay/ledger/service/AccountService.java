package com.swiftpay.ledger.service;

import com.swiftpay.ledger.domain.Account;

/** Account lifecycle port — load existing or create a new account on demand. */
public interface AccountService {
    Account loadOrCreate(String userId, String currency);
}
