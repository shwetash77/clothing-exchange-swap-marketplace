# Swap Marketplace

A backend for a clothing exchange/swap marketplace, built to demonstrate
production-relevant backend concerns: RBAC, concurrency-safe state
transitions, and an escrow-style payment flow — not just CRUD.

## Stack
Java 21 · Spring Boot 3 · Spring Security (JWT) · PostgreSQL · Razorpay API ·
Docker · Kubernetes

## Core design decisions (be ready to explain these in an interview)

**Concurrency-safe item locking.** `Item` carries a `@Version` column.
`SwapService.acceptSwap()` is `@Transactional`; if two swap requests for the
same item are accepted concurrently, Hibernate's optimistic-lock check
causes the losing transaction to throw `ObjectOptimisticLockingFailureException`,
which `GlobalExceptionHandler` turns into a clean `409`. See the comment
block on `acceptSwap()` for the full reasoning, including the trade-off vs.
pessimistic locking (`ItemRepository.findByIdForUpdate`, also implemented).

**RBAC.** Three roles — `USER`, `SELLER`, `ADMIN` — enforced two ways:
URL-pattern rules in `SecurityConfig` (e.g. `/admin/**` requires `ADMIN`)
and method-level `@PreAuthorize` on individual endpoints (e.g. only
`SELLER`/`ADMIN` can create item listings).

**Escrow payment flow (Razorpay).** Deposits use a Razorpay Order created
with `payment_capture = 0` — the card is authorized (funds held) but not
charged. Funds are only captured when both parties confirm the swap
(`releaseDeposit`), or the authorization is refunded on dispute/cancellation
(`refundDeposit`). Webhook events are verified via HMAC signature
(`Utils.verifyWebhookSignature`) and handled idempotently — replayed
Razorpay webhooks are detected and no-op'd rather than double-processed.

## Local development

```bash
docker compose up --build
```

This starts Postgres and the app together. API is available at
`http://localhost:8080`.

## Running on Kubernetes (Minikube)

```bash
minikube start
minikube image build -t swap-marketplace:latest .
kubectl apply -f k8s/config-and-secrets.yaml
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/app-deployment.yaml
minikube service swap-marketplace-service --url
```

## Environment variables

| Variable | Purpose |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Postgres connection |
| `JWT_SECRET` | HMAC signing key for JWTs (32+ bytes) |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | Razorpay API key pair (test mode: `rzp_test_...`) |
| `RAZORPAY_WEBHOOK_SECRET` | Used to verify incoming webhook signatures |

## API overview

```
POST   /auth/signup
POST   /auth/login
GET    /items?category=
POST   /items                          [SELLER, ADMIN]
POST   /swap-requests
PATCH  /swap-requests/{id}/accept      <- concurrency-critical
PATCH  /swap-requests/{id}/cancel
POST   /payments/deposit
POST   /payments/{swapRequestId}/release
POST   /payments/{swapRequestId}/refund
POST   /payments/webhook               (Razorpay signature-verified, public)
```

## Sprint log

- **Sprint 1** — Auth (JWT), User entity + RBAC roles, Item CRUD
- **Sprint 2** — Swap request flow, optimistic-locking accept logic
- **Sprint 3** — Razorpay escrow integration (deposit/release/refund + webhook)
- **Sprint 4** — Dockerized, deployed to local Kubernetes (Minikube)
- **Sprint 5** — AWS deployment, polish (reviews, admin endpoints)

## Known simplifications (called out intentionally, not hidden)

- `ddl-auto: update` is used for convenience; a real deployment would use
  Flyway/Liquibase migrations instead.
- `releaseDeposit` does a full capture for simplicity; a real platform
  would typically release the deposit and capture only a small fee.
- No refresh-token rotation — access tokens are long-lived (24h) for demo
  simplicity.
