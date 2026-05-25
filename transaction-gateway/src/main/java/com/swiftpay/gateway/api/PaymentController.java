package com.swiftpay.gateway.api;

import com.swiftpay.gateway.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "P2P payment endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Submit a P2P payment request")
    public ResponseEntity<PaymentResponse> submit(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.submit(request);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Fetch payment status")
    public ResponseEntity<PaymentResponse> get(@PathVariable String transactionId) {
        return ResponseEntity.ok(paymentService.get(transactionId));
    }
}
