package com.swiftpay.gateway.service.impl;

import com.swiftpay.gateway.api.PaymentRequest;
import com.swiftpay.gateway.api.PaymentResponse;
import com.swiftpay.gateway.domain.Payment;
import com.swiftpay.gateway.domain.PaymentRepository;
import com.swiftpay.gateway.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Default {@link PaymentService} implementation.
 *
 * <p>Pure orchestrator — single responsibility is the <em>submission workflow</em>:
 * idempotency → validation → persistence → event publish. Each collaborator is
 * injected through an interface (DIP), keeping this class open for extension
 * (new balance sources, alt publishers) and closed for modification (OCP).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository repository;
    private final IdempotencyService idempotencyService;
    private final BalanceLookup balanceLookup;
    private final PaymentEventPublisher eventPublisher;
    private final PaymentMapper mapper;

    @Override
    @Transactional
    public PaymentResponse submit(PaymentRequest request) {
        if (!idempotencyService.reserve(request.transactionId())) {
            throw new DuplicateTransactionException(
                    "Transaction already submitted: " + request.transactionId());
        }

        BigDecimal balance = balanceLookup.balanceOf(request.senderId());
        if (balance.compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException(
                    "Sender " + request.senderId() + " has insufficient funds");
        }

        Payment saved = repository.save(mapper.toEntity(request));
        eventPublisher.publishInitiated(saved);

        log.info("Payment {} accepted (PENDING)", saved.getTransactionId());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse get(String transactionId) {
        return repository.findById(mapper.parseUuid(transactionId))
                .map(mapper::toResponse)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found: " + transactionId));
    }
}
