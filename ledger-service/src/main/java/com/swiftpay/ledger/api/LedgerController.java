package com.swiftpay.ledger.api;

import com.swiftpay.ledger.domain.LedgerEntry;
import com.swiftpay.ledger.domain.LedgerEntryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/ledger")
@RequiredArgsConstructor
@Tag(name = "Ledger")
public class LedgerController {

    private final LedgerEntryRepository repository;

    @GetMapping("/users/{userId}/history")
    @Operation(summary = "Fetch transaction history for a user")
    public List<LedgerEntry> history(@PathVariable String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
