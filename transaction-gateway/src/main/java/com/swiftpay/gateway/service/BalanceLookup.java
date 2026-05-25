package com.swiftpay.gateway.service;

import java.math.BigDecimal;

/**
 * Balance-lookup port (DIP). Swap the implementation to back the lookup
 * with Redis, a ledger REST call, or gRPC without touching callers.
 */
public interface BalanceLookup {
    BigDecimal balanceOf(String userId);
}
