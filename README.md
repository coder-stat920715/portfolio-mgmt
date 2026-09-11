# Portfolio Management System

A production-grade **Spring Boot 3.x / Hibernate 6.x** reference project modeling a **Financial Portfolio & Order Management System** — built as a hands-on interview-preparation repository for Senior Java Developer roles (JP Morgan, Persistent Systems, Mphasis, and similar enterprise stacks).

Repository: [github.com/coder-stat920715/portfolio-mgmt](https://github.com/coder-stat920715/portfolio-mgmt)

Every non-trivial annotation/design decision in this codebase is documented **in-line** with an "interview concept" comment explaining *why* it's used and what trade-off it represents — this README ties those pieces together at a project level.

---

## Table of Contents

- [Domain Model](#domain-model)
- [Key Concepts Demonstrated](#key-concepts-demonstrated)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration Profiles](#configuration-profiles)
- [REST API Reference](#rest-api-reference)
- [Postman Collection](#postman-collection)
- [Running the Tests](#running-the-tests)
- [Observing Locking & Caching Behavior](#observing-locking--caching-behavior)
- [H2 Console](#h2-console)
- [Roadmap / Ideas for Extension](#roadmap--ideas-for-extension)

---

## Domain Model

```
User (1) ────< (N) Account (1) ────< (N) Portfolio (1) ────< (N) PortfolioAsset >──── (1) Asset
                     │                                              │                    │
                     │                                              │            ┌───────┴────────┐
                     │                                              │       EquityAsset      FixedIncomeAsset
                     │                                              │        (JOINED inheritance under Asset)
                     └────< (N) TradeOrder >──── (1) Asset           │
                     └────< (N) Transaction ──── (1) TradeOrder      │
                                                                      PortfolioAsset (@EmbeddedId join entity:
                                                                      allocationPercentage, units)

AuditLog — independent business-audit trail, written via REQUIRES_NEW so it survives
           rollbacks of the transaction that triggered it.
```

| Entity | Purpose |
|---|---|
| `User` | Account holder. Bidirectional `@OneToMany`/`@ManyToOne` to `Account`. |
| `Account` | Cash balance. `@Version` optimistic lock; `@ManyToOne` to `User`; `@OneToMany` to `Portfolio`. |
| `Portfolio` | Named grouping of holdings for an `Account`. L2-cached (`READ_WRITE`). |
| `Asset` | Abstract base, `@Inheritance(strategy = JOINED)`, `@Cache` L2-cached read-heavy reference data. |
| `EquityAsset` / `FixedIncomeAsset` | Concrete subclasses with their own joined tables. |
| `PortfolioAsset` | Explicit many-to-many join entity with `@EmbeddedId` (`PortfolioAssetId`) carrying `allocationPercentage` and `units`. |
| `TradeOrder` | BUY/SELL instruction; the high-contention entity locked with `PESSIMISTIC_WRITE` during execution. |
| `Transaction` | Immutable ledger row produced by a successfully executed `TradeOrder`. |
| `AuditLog` | Business-level audit trail, independent of JPA's technical `@CreatedDate`/`@LastModifiedDate` auditing. |

---

## Key Concepts Demonstrated

**Entity Mapping & Inheritance**
- Bidirectional `@OneToMany`/`@ManyToOne` with `mappedBy`, `orphanRemoval`, and explicit `CascadeType`
- `@ManyToMany` modeled as an explicit join entity (`PortfolioAsset`) using `@EmbeddedId` + `@MapsId`
- `@Inheritance(strategy = InheritanceType.JOINED)` on `Asset` → `EquityAsset` / `FixedIncomeAsset`
- `FetchType.LAZY` enforced across every association

**Querying & Optimization**
- N+1 fixed two ways: JPQL `JOIN FETCH` (`PortfolioRepository#findAllWithAssetsByAccountId`) and `@EntityGraph` (`PortfolioRepository#findWithAssetsById`)
- Dynamic, type-safe queries via **JPA Criteria API** (`TradeOrderRepositoryImpl`) and **Spring Data Specifications** (`AssetSpecifications`)
- `Pageable` / `Page<T>` pagination and sorting throughout
- Native SQL query mapped to an **interface DTO projection** (`AccountBalanceProjection`)

**Concurrency, Caching & Auditing**
- **Optimistic locking** (`@Version`) with retry-on-conflict in `AccountService`
- **Pessimistic locking** (`LockModeType.PESSIMISTIC_WRITE`) for trade execution in `TradeExecutionService`
- **Hibernate L2 cache** via Caffeine/JCache on read-heavy `Asset` entities
- Automatic `@CreatedDate` / `@LastModifiedDate` auditing via `AuditingEntityListener`

**Transactions & Persistence Context**
- `Propagation.REQUIRED` vs. `Propagation.REQUIRES_NEW` (`AuditLogService` commits independently of the caller's rollback)
- Explicit isolation levels (`READ_COMMITTED`, `SERIALIZABLE`)
- Mixed Spring Data repositories + a hand-rolled `EntityManager`/Criteria-based custom repository implementation (`TradeOrderRepositoryImpl`)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 17 |
| Framework | Spring Boot 3.3.x |
| Persistence | Spring Data JPA + Hibernate 6.5.x |
| Database | H2 (in-memory, default) / PostgreSQL (`postgres` profile) |
| L2 Cache | Hibernate `hibernate-jcache` + Caffeine JCache provider |
| Build | Maven |
| Testing | JUnit 5, `@DataJpaTest`, `@SpringBootTest`, AssertJ |
| Boilerplate reduction | Lombok |

---

## Project Structure

```
portfolio-mgmt/
├── pom.xml
├── postman/
│   └── Portfolio-Management-System.postman_collection.json
├── src/main/java/com/interview/portfolio/
│   ├── PortfolioMgmtApplication.java
│   ├── entity/          # User, Account, Portfolio, Asset hierarchy, PortfolioAsset, TradeOrder, Transaction, AuditLog
│   ├── dto/              # Request/response DTOs, projections, search criteria
│   ├── repository/       # Spring Data repositories, Specifications, custom EntityManager impl
│   ├── service/           # AccountService, TradeExecutionService, PortfolioService, AuditLogService
│   ├── controller/        # AccountController, TradeOrderController, PortfolioController
│   ├── exception/         # Domain exceptions + GlobalExceptionHandler
│   └── config/            # JpaAuditingConfig, CacheConfig
├── src/main/resources/
│   ├── application.yml     # default (H2) + postgres profile
│   └── caffeine.properties # per-region L2 cache tuning
└── src/test/java/com/interview/portfolio/
    ├── repository/    # cascade/orphanRemoval, N+1 query-count, auditing lifecycle
    ├── cache/         # L2 cache hit/miss verification via Hibernate Statistics
    └── concurrency/   # optimistic-retry vs. pessimistic-lock integration tests
```

---

## Getting Started

### Prerequisites
- JDK 17+
- Maven 3.9+ (or use the included wrapper if you add one)

### Clone & Run

```bash
git clone https://github.com/coder-stat920715/portfolio-mgmt.git
cd portfolio-mgmt
mvn clean install
mvn spring-boot:run
```

The app starts on **`http://localhost:8080`** using an in-memory H2 database (schema auto-created, no external DB needed).

### Build a runnable jar

```bash
mvn clean package
java -jar target/portfolio-mgmt-1.0.0.jar
```

---

## Configuration Profiles

| Profile | Datasource | Notes |
|---|---|---|
| *(default)* | H2 in-memory (`jdbc:h2:mem:portfoliodb`) | `ddl-auto: update`, ideal for local exploration/demo |
| `postgres` | PostgreSQL (`jdbc:postgresql://localhost:5432/portfoliodb`) | `ddl-auto: validate` — run migrations yourself; update credentials in `application.yml` |

Activate the Postgres profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

`application.yml` also enables, for interview/demo visibility:
- `hibernate.show_sql` + `format_sql` + `highlight_sql`
- `hibernate.generate_statistics` (query counts, cache hit/miss counters — surfaced in the test suite)
- Hibernate L2 cache via JCache/Caffeine, configured per-region in `caffeine.properties`
- `open-in-view: false` (deliberately disabled to force explicit fetch planning rather than relying on the OSIV anti-pattern)

---

## REST API Reference

Base URL: `http://localhost:8080`

### Accounts

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/accounts/{id}` | Fetch an account by ID |
| `POST` | `/api/accounts/{id}/adjust-balance?delta={amount}` | Deposit (positive `delta`) or withdraw (negative `delta`); optimistic-locked with retry |

### Trade Orders

| Method | Path | Body | Description |
|---|---|---|---|
| `POST` | `/api/trade-orders` | `{ "accountId": ..., "assetId": ..., "orderType": "BUY|SELL", "quantity": ... }` | Executes a trade under a pessimistic row lock on the account |

### Portfolios & Assets

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/portfolios/account/{accountId}/summaries` | All portfolio summaries for an account (JOIN FETCH — 1 query) |
| `GET` | `/api/portfolios/{portfolioId}/summary` | Single portfolio summary (`@EntityGraph` — 1 query) |
| `GET` | `/api/portfolios/account/{accountId}?page=&size=&sort=` | Paginated `Portfolio` list |
| `GET` | `/api/portfolios/assets/search?symbolContains=&assetType=&minPrice=&maxPrice=&sector=&page=&size=&sort=` | Dynamic Specification-based asset search, paginated |

All error responses follow a consistent shape (`GlobalExceptionHandler`):

```json
{
  "timestamp": "2026-09-11T10:15:30",
  "status": 404,
  "error": "Not Found",
  "message": "Account not found: 99",
  "path": "/api/accounts/99"
}
```

| Exception | HTTP Status |
|---|---|
| `ResourceNotFoundException` | 404 |
| `InsufficientBalanceException` | 422 |
| `OptimisticLockingFailureException` / `TradeConflictException` | 409 |
| `PessimisticLockingFailureException` / `LockAcquisitionException` | 503 |
| Validation errors | 400 |

---

## Postman Collection

A ready-to-import collection covering every endpoint above lives at:

```
postman/Portfolio-Management-System.postman_collection.json
```

**Import it:** Postman → *Import* → select the file (or drag it in).

It includes:
- One request per endpoint, with example request bodies/query params
- A `baseUrl` collection variable (defaults to `http://localhost:8080`) plus `accountId`, `assetId`, `portfolioId` variables you can update after seeding data
- Dedicated **"Concurrency Demo"** requests for both the Account (optimistic lock) and Trade Order (pessimistic lock) endpoints, meant to be fired repeatedly/concurrently via Postman's **Collection Runner** to observe the two different locking strategies in your server logs
- An "Insufficient Funds Demo" request that proves a rejected trade still leaves a durable `AuditLog` row (via `REQUIRES_NEW`) even though the HTTP call itself returns an error

> The project ships with no seed data script — create a `User` → `Account` → `Asset`/`Portfolio` via the H2 console or your own bootstrap `CommandLineRunner` before exercising the collection, and update the collection variables to match the IDs you get back.

---

## Running the Tests

```bash
mvn test
```

| Test class | What it proves |
|---|---|
| `CascadeAndOrphanRemovalTest` | Cascade persist/delete and `orphanRemoval` actually issue the expected INSERT/DELETE statements |
| `AuditingLifecycleTest` | `@CreatedDate`/`@LastModifiedDate` populate on insert and update correctly |
| `NPlusOneQueryCountTest` | JOIN FETCH and `@EntityGraph` each execute **exactly one** SQL query (via Hibernate `Statistics.getPrepareStatementCount()`) |
| `L2CacheTest` | A second lookup after clearing the persistence context is served from the L2 cache, not the database (via `Statistics.getSecondLevelCacheHitCount()`) |
| `OptimisticLockingConcurrencyTest` | 10 concurrent balance adjustments against the same account all succeed (via retry) with a correct final balance |
| `PessimisticLockingConcurrencyTest` | 8 concurrent trades against the same account are serialized by the row lock with zero lost updates |

---

## Observing Locking & Caching Behavior

1. Set `logging.level.org.hibernate.SQL: DEBUG` (already enabled) and watch the console while running the concurrency tests or firing concurrent Postman requests.
2. For **optimistic locking**: repeatedly `POST /api/accounts/{id}/adjust-balance` concurrently — you'll see `ObjectOptimisticLockingFailureException` caught and retried inside `AccountService`, and no failed requests reach the client.
3. For **pessimistic locking**: repeatedly `POST /api/trade-orders` concurrently against the same `accountId` — requests will complete sequentially (queued on the row lock) rather than racing.
4. For **L2 cache**: hit `GET /api/portfolios/assets/search` twice for the same asset and compare Hibernate statistics logs, or run `L2CacheTest`.

---

## H2 Console

With the default profile running, open **http://localhost:8080/h2-console** in a browser:

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:mem:portfoliodb` |
| Username | `sa` |
| Password | *(blank)* |

---

## Roadmap / Ideas for Extension

- Add Flyway/Liquibase migrations for the `postgres` profile (currently `ddl-auto: validate` assumes a pre-existing schema)
- Add Spring Security + JWT and wire a real `AuditorAware` (currently stubbed to `"SYSTEM"` in `JpaAuditingConfig`)
- Add a `CommandLineRunner`/`data.sql` seed script so the Postman collection works out of the box
- Add Testcontainers-based integration tests against real PostgreSQL to validate `SERIALIZABLE` isolation and `FOR UPDATE` locking beyond H2's semantics
- Add OpenAPI/Swagger (`springdoc-openapi`) for interactive API docs alongside the Postman collection

---

*This project is intended as a study/reference resource for Hibernate and Spring Data JPA interview preparation — not a production trading system. Trade execution, pricing, and settlement logic are deliberately simplified.*
