# Surense Support Hub

Customer support ticket management API — senior backend take-home assignment.

**Stack:** Java 21 · Spring Boot 3.3 · MySQL 8.4 · Flyway · JWT RS256 · MapStruct · Testcontainers

---

## Quick Start (Docker)

```bash
docker compose up --build
```

The app starts on port **8080**. The default admin user is seeded by Flyway:

| Field    | Value              |
|----------|--------------------|
| email    | admin@surense.com  |
| password | Admin@1234         |

Swagger UI: <http://localhost:8080/swagger-ui.html>

---

## Running Locally (without Docker)

**Prerequisites:** Java 21, a running MySQL 8.4 instance.

```bash
export DB_URL=jdbc:mysql://localhost:3306/surense
export DB_USERNAME=surense
export DB_PASSWORD=surense
./gradlew bootRun
```

### Environment Variables

| Variable       | Required | Description                                      |
|----------------|----------|--------------------------------------------------|
| `DB_URL`       | yes      | JDBC URL for MySQL                               |
| `DB_USERNAME`  | yes      | Database user                                    |
| `DB_PASSWORD`  | yes      | Database password                                |
| `JWT_PRIVATE_KEY` | prod  | RSA private key PEM (auto-generated in dev)     |
| `JWT_PUBLIC_KEY`  | prod  | RSA public key PEM (auto-generated in dev)      |

---

## Running Tests

```bash
./gradlew clean test
```

10 test suites. Testcontainers pulls a MySQL 8.4 image automatically — Docker must be running.

---

## API Overview

All API endpoints require a `Bearer` JWT unless noted.

### Auth (`/auth`)

| Method | Path             | Auth     | Description                          |
|--------|------------------|----------|--------------------------------------|
| POST   | /auth/login      | public   | Returns access token + refresh token |
| POST   | /auth/refresh    | public   | Rotates refresh token                |
| POST   | /auth/logout     | public   | Revokes refresh token family         |

### Agents (`/api/v1/agents`)

| Method | Path                  | Role        | Description                        |
|--------|-----------------------|-------------|------------------------------------|
| GET    | /api/v1/agents/me     | AGENT       | Get own profile                    |
| PATCH  | /api/v1/agents/me     | AGENT       | Update own profile                 |
| POST   | /api/v1/agents        | ADMIN       | Create agent                       |
| GET    | /api/v1/agents        | ADMIN       | List all agents                    |
| PATCH  | /api/v1/agents/{id}   | ADMIN       | Update any agent                   |

### Customers (`/api/v1/customers`)

| Method | Path                     | Role             | Description                                       |
|--------|--------------------------|------------------|---------------------------------------------------|
| POST   | /api/v1/customers        | AGENT/ADMIN      | Create customer (AGENT: self-assigned; ADMIN: supply agentId) |
| GET    | /api/v1/customers        | AGENT/ADMIN      | List (AGENT sees own; ADMIN sees all)             |
| GET    | /api/v1/customers/me     | CUSTOMER         | Get own profile                                   |
| PATCH  | /api/v1/customers/me     | CUSTOMER         | Update own profile                                |
| GET    | /api/v1/customers/{id}   | ADMIN/AGENT      | Get by ID (ADMIN: any; AGENT: ownership enforced) |
| PATCH  | /api/v1/customers/{id}   | ADMIN            | Update any customer                               |

### Tickets (`/api/v1/tickets`)

| Method | Path                   | Role             | Description                                       |
|--------|------------------------|------------------|---------------------------------------------------|
| POST   | /api/v1/tickets        | CUSTOMER         | Create ticket                                     |
| GET    | /api/v1/tickets        | CUSTOMER/AGENT/ADMIN | List (scoped by role; filter: status, priority)|
| GET    | /api/v1/tickets/{id}   | CUSTOMER/AGENT/ADMIN | Get by ID (ownership enforced)                |
| PATCH  | /api/v1/tickets/{id}   | AGENT            | Update status / note (ownership enforced)         |

### Actuator

| Path                          | Auth        | Description      |
|-------------------------------|-------------|------------------|
| /actuator/health/liveness     | public      | Liveness probe   |
| /actuator/health/readiness    | public      | Readiness + DB   |
| /actuator/metrics             | ADMIN       | Micrometer metrics|
| /actuator/info                | ADMIN       | App info         |

---

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for decisions on security model, refresh-token rotation, HikariCP tuning, and observability.

Sample HTTP requests are in [requests.http](requests.http) (IntelliJ HTTP Client / VS Code REST Client).
