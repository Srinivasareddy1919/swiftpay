# SwiftPay — Real-Time Payment Ledger (Hackathon POC)

A resilient, event-driven P2P payment platform composed of three Spring Boot services.

```
client -> [Service A: Transaction Gateway] --Kafka--> [Service B: Ledger] --Kafka--> [Service C: Analytics]
              |               |                              |
            Redis          Postgres                        Postgres
        (idempotency)    (payments)                      (ledger_entries, accounts)
```

## Stack

| Concern         | Technology                                |
|-----------------|-------------------------------------------|
| Language        | Java 21, Spring Boot 3.3                  |
| DB              | PostgreSQL 16 (Flyway migrations)         |
| Messaging       | Apache Kafka (KRaft)                      |
| Cache / Idemp.  | Redis 7                                   |
| API Docs        | springdoc-openapi (Swagger UI)            |
| Tests           | JUnit 5 + Testcontainers                  |
| Infra           | Docker Compose                            |
| CI              | GitHub Actions                            |
| Load Test       | k6 (`load-test/swiftpay-load.js`)         |

## Modules

- `transaction-gateway` (port **8081**) — REST API, Redis idempotency, Kafka producer
- `ledger-service` (port **8082**) — Kafka consumer, atomic debit/credit, retry on transient DB errors, history endpoint
- `analytics-worker` (port **8083**) — Bonus OLAP-style consumer + summary endpoint

## Run end-to-end

```bash
docker compose up --build
```

Then:

- Swagger UI: <http://localhost:8081/swagger-ui.html>, <http://localhost:8082/swagger-ui.html>
- Health: `GET /actuator/health` on each service
- Submit a payment:

```bash
curl -X POST http://localhost:8081/v1/payments \
  -H 'Content-Type: application/json' \
  -d '{
    "transactionId":"11111111-1111-1111-1111-111111111111",
    "senderId":"user-1","receiverId":"user-2",
    "amount":"42.50","currency":"USD"
  }'
```

- Fetch history: `GET http://localhost:8082/v1/ledger/users/user-1/history`
- Analytics summary: `GET http://localhost:8083/v1/analytics/summary`

## Build & Test

```bash
mvn -B clean verify
```

Integration tests in `transaction-gateway` spin up Postgres, Kafka, and Redis via Testcontainers.

## Load test (250 TPS × 1,000,000 transactions)

1. Start the stack: `docker compose up -d --build`
2. Start PCAP capture **on the host** in parallel (so it records the entire test):
   - **Linux:**
     ```bash
     sudo tcpdump -i any -w swiftpay-load.pcap \
       'port 8081 or port 8082 or port 9092 or port 5432 or port 6379'
     ```
   - **Windows:** open Wireshark, choose the loopback adapter (`Npcap Loopback`) and apply the same display filter, then **File → Save As → swiftpay-load.pcap**.
3. Run k6:
   ```bash
   k6 run --out json=results.json load-test/swiftpay-load.js
   ```
4. Stop the capture (`Ctrl+C` in tcpdump / **Stop** in Wireshark).
5. Commit the PCAP file:
   ```bash
   git add swiftpay-load.pcap results.json
   git commit -m "load test: 250 TPS x 1M transactions"
   git push
   ```

> ⚠️ The PCAP captures real network traffic — keep credentials out of headers (the demo uses no auth).

## Resilience Notes

- **Kafka consumer** uses manual ack + `@Retryable` with exponential backoff for transient DB issues.
- **Idempotency** uses Redis `SETNX` with 24h TTL; duplicates return **HTTP 409**.
- **Atomic ledger** updates use `PESSIMISTIC_WRITE` on accounts and a unique `(transaction_id, user_id, direction)` constraint to prevent double-posting.
- **Producer** uses `acks=all` + `enable.idempotence=true`.

## Error Handling

| Scenario                | Status | Code                    |
|-------------------------|--------|--------------------------|
| Validation failure      | 400    | `VALIDATION_ERROR`       |
| Duplicate transactionId | 409    | `DUPLICATE_TRANSACTION`  |
| Insufficient funds      | 422    | `INSUFFICIENT_FUNDS`     |
| Payment not found       | 404    | `NOT_FOUND`              |
| Unhandled               | 500    | `INTERNAL_ERROR`         |

## Project Layout

```
swiftpay/
├── docker-compose.yml
├── .github/workflows/ci.yml
├── load-test/swiftpay-load.js
├── transaction-gateway/
├── ledger-service/
└── analytics-worker/
```
