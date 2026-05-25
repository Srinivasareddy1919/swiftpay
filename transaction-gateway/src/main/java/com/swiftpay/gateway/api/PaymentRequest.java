package com.swiftpay.gateway.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank String transactionId,
        @NotBlank String senderId,
        @NotBlank String receiverId,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency
) {}
