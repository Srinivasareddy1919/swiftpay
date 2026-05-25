package com.swiftpay.gateway.service;

import com.swiftpay.gateway.api.PaymentRequest;
import com.swiftpay.gateway.api.PaymentResponse;

/**
 * Application service port for payment submission/lookup (DIP).
 * Controllers depend on this interface, never on a concrete implementation.
 */
public interface PaymentService {
    PaymentResponse submit(PaymentRequest request);
    PaymentResponse get(String transactionId);
}
