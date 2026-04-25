# Architecture Notes

## Overview

Surense is a monolithic Spring Boot 3.3 service. There are no external brokers (no Kafka, Redis, or RabbitMQ). All complexity lives inside a single deployable JAR. This choice trades horizontal scalability at the messaging layer for operational simplicity appropriate for the project scope.

## Security Model

**Authentication:** JWT RS256 access tokens (15-minute TTL) + opaque refresh tokens (14-day TTL, SHA-256 hashed in DB). The RSA keypair is auto-generated at startup in dev; in production it must be injected via environment variables.

**Refresh-token rotation:** Each `POST /auth/refresh` call invalidates the consumed token and issues a new one. `SELECT ... FOR UPDATE` is used when loading the token to prevent concurrent rotation races.

**Authorization:** Spring Security method security with `@PreAuthorize`. A custom `OwnershipEvaluator` enforces ownership checks beyond role (e.g., an AGENT can only read their own customers; a CUSTOMER can only read their own tickets). `agentId`/`customerId` are always pulled from the JWT claims — never trusted from the request body or path.

## Error Responses

All errors are RFC 7807 `ProblemDetail` with a `traceId` property stamped from MDC. Every error path through `GlobalExceptionHandler` calls `withTraceId()`. Refresh-token errors intentionally return identical 401 responses for all failure modes to avoid oracle attacks.

## Data Access

- `ddl-auto=validate`; Flyway owns all schema migrations.
- `@EntityGraph` or fetch joins on collection-loading queries to prevent N+1.
- `@Transactional(readOnly=true)` on all query service methods.
- JPA entities are never returned from controllers; all API surfaces use Java records as DTOs.

## Connection Pool (HikariCP)

Spring Boot auto-configures HikariCP with sensible defaults (`maximumPoolSize=10`, `connectionTimeout=30s`, `idleTimeout=600s`). These defaults are intentionally left unchanged.

**Tuning guidance (Tier 3 concern — do not change without load data):**

- `maximumPoolSize` should follow `(core_count * 2) + effective_spindle_count`. For a single-core container pointing at a managed MySQL instance, 10 is a reasonable starting point.
- `minimumIdle=maximumPoolSize` (pool kept warm) is appropriate for a latency-sensitive API; lower it only if you need to release connections during sustained idle periods.
- `connectionTimeout` defaults to 30 s. In production, lower to 5–10 s so callers fail fast rather than queuing behind a saturated pool.
- Monitor `hikaricp.connections.pending` and `hikaricp.connections.timeout` via `/actuator/metrics` (ADMIN role required) before adjusting pool size.

## Observability

- All log lines include `traceId` from MDC (set by `CorrelationIdFilter`). In production logs are emitted as structured JSON via `logstash-logback-encoder` for ingestion by log aggregators.
- Login and logout events are logged at INFO level with userId and source IP for traceability.
- Liveness probe: `GET /actuator/health/liveness` — no auth required.
- Readiness probe: `GET /actuator/health/readiness` — checks `livenessState` + `db`. No auth required.
- Metrics and info endpoints (`/actuator/info`, `/actuator/metrics/**`) require `ROLE_ADMIN`.

## Graceful Shutdown

`server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase=30s`. On SIGTERM, Spring drains in-flight requests for up to 30 s before the JVM exits. This allows Kubernetes rolling deploys to complete cleanly without 502s.
