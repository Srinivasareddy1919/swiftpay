# SwiftPay — Coding Standards & Architecture

This document records the design rules every module follows.

## 1. Layered Architecture

```
api      ── REST/Kafka adapters (controllers, listeners, exception handlers, DTOs)
service  ── application service *ports* (interfaces) — business orchestration
service/impl ── concrete *adapters* implementing the ports
domain   ── entities, value objects, repositories
event    ── domain & integration events (records)
```

Dependencies only point **inward**: `api → service → domain`. `service/impl` may
depend on infrastructure (Kafka, Redis, JPA) but never the other way.

## 2. SOLID

| Principle | Where it shows up |
|-----------|-------------------|
| **S — Single Responsibility** | `PaymentService` orchestrates; `IdempotencyService` reserves keys; `BalanceLookup` reads balances; `PaymentEventPublisher` sends events; `PaymentMapper` converts DTO↔entity. Same split in `ledger-service` (`LedgerService`, `AccountService`, `LedgerEventPublisher`). |
| **O — Open/Closed** | New balance sources (REST, gRPC) plug in by adding another `BalanceLookup` implementation — no change to `PaymentServiceImpl`. |
| **L — Liskov Substitution** | All implementations honour the contract of their port; tests can substitute `Redis*` with in-memory fakes. |
| **I — Interface Segregation** | Each port exposes one cohesive verb (`reserve`, `balanceOf`, `publishInitiated`) instead of fat utility classes. |
| **D — Dependency Inversion** | High-level services depend on `service.*` interfaces; the Kafka/Redis adapters live in `service.impl.*` and are wired by Spring. |

## 3. Design Patterns Applied

- **Hexagonal / Ports & Adapters** — `service` defines ports, `service.impl` and `api` are adapters.
- **Repository** — `PaymentRepository`, `AccountRepository`, `LedgerEntryRepository`, `PaymentFactRepository`.
- **DTO + Mapper** — `PaymentRequest/Response` + `PaymentMapper` keep transport types out of the domain.
- **Publisher** — `PaymentEventPublisher` / `LedgerEventPublisher` decouple business logic from Kafka.
- **Builder** — Lombok `@Builder` on entities for safe immutability at construction.
- **Strategy** (latent) — Multiple `BalanceLookup` implementations can be selected via configuration.
- **Retry / Circuit-friendly Consumer** — `@Retryable` with exponential backoff on the Kafka listener.

## 4. Coding Rules

- Constructor injection only (`@RequiredArgsConstructor` + `final`). No field `@Autowired`.
- Records for DTOs and events; entities use focused Lombok annotations (no `@Data`).
- `@Transactional(readOnly = true)` on pure reads; default for writes.
- No raw string keys in business code — Redis prefixes / topic names are constants or `@Value`s.
- Validation via `jakarta.validation` annotations on request DTOs; errors mapped by `GlobalExceptionHandler`.
- Structured logging (`Slf4j`); no `System.out`.
- Exceptions are domain-named (`DuplicateTransactionException`, `InsufficientFundsException`, `PaymentNotFoundException`) and mapped to standard HTTP codes.

## 5. Package Map

### transaction-gateway
```
com.swiftpay.gateway
├── TransactionGatewayApplication
├── api/         PaymentController, PaymentRequest, PaymentResponse,
│                ApiError, GlobalExceptionHandler
├── service/     PaymentService, IdempotencyService, BalanceLookup,
│                PaymentEventPublisher, PaymentMapper, *Exception
│   └── impl/    PaymentServiceImpl, RedisIdempotencyService,
│                RedisBalanceLookup, KafkaPaymentEventPublisher
├── domain/      Payment, PaymentStatus, PaymentRepository
└── event/       PaymentInitiatedEvent
```

### ledger-service
```
com.swiftpay.ledger
├── LedgerServiceApplication
├── api/         LedgerController
├── service/     LedgerService, AccountService, LedgerEventPublisher
│   └── impl/    LedgerServiceImpl, AccountServiceImpl, KafkaLedgerEventPublisher
├── domain/      Account, AccountRepository, LedgerEntry, LedgerEntryRepository
├── event/       PaymentInitiatedEvent, PaymentCompletedEvent
└── kafka/       PaymentInitiatedListener
```

### analytics-worker
```
com.swiftpay.analytics
├── AnalyticsWorkerApplication
├── api/         AnalyticsController
├── service/     AnalyticsService
│   └── impl/    AnalyticsServiceImpl
├── domain/      PaymentFact, PaymentFactRepository
├── event/       PaymentCompletedEvent
└── kafka/       PaymentCompletedListener
```
