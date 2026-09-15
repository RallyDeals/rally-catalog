# Rally Catalog Service

Catalog Service owns product listings, category taxonomy, product moderation,
and buyer search/browse for the RallyDeals platform.

**Spec reference:** `rally-docs/services docs/catalog-service.md` (requirements)
and `rally-docs/services docs` + `groupdeal-architecture.md` §4.3 (architecture).
This README documents the **implementation** — what exists, how to run it, and
what is still needed.

---

## Tech Stack

- **Java 21 / Spring Boot 3.5**
- **PostgreSQL 16** — persistence, managed via Flyway migrations
- **Spring Security** — present but trusts gateway-injected identity headers
  (see [Identity & Gateway](#identity--api-gateway-contract))
- **Flyway** — versioned DB schema + seed data
- **Lombok** — DTO/entity boilerplate
- **MapStruct** — compile-time entity → DTO mapping (`CatalogMapper`, same setup as order-service)
- **rally-common** — shared `BaseException` hierarchy + `JwtService`
- **Micrometer Tracing (OpenTelemetry)** — W3C `traceparent` propagation on HTTP/Kafka + OTLP export
- **logback-spring.xml** — context-aware logging: plain-text console (local/dev), Logstash JSON console (prod), rolling file, Loki appender
- **Correlation-ID plumbing** — `X-Correlation-Id` propagated across inbound HTTP, outbound REST, and Kafka (mirrors `rally-order` / `rally-payment`)
- **H2** (test scope) + JUnit 5 / Mockito for unit tests

---

## Project Structure

```
rally-catalog/
├── Dockerfile                    # multi-stage: installs rally-common v0.2.0 from GitHub, packages jar
├── docker-compose.yml            # catalog-db only (host 5433); catalog-service commented out for local IntelliJ dev
├── postman/
│   └── rally-catalog.postman_collection.json   # importable end-to-end test collection
└── src/
    ├── main/
    │   ├── java/com/rally/catalog/
    │   │   ├── CatalogServiceApplication.java
    │   │   ├── config/SecurityConfig.java      # permitAll; AdminRoleFilter (auto-registered) guards /products/admin/*
    │   │   ├── config/KafkaProducerConfig.java # StringSerializer + JSON, correlation interceptor, observation enabled
    │   │   ├── config/rest/                     # RestClientConfig + CorrelationIdRequestInterceptor (outbound REST)
    │   │   ├── controller/                      # CategoryController, ProductController, InternalProductController
    │   │   ├── dto/                             # request/response DTOs (PageResponse, SellerSummary, ...)
    │   │   ├── entity/                          # Category, Product, ProductStatus, Role
    │   │   ├── event/                           # ProductCreatedEvent, ProductDeletedEvent
    │   │   ├── exception/GoneException.java     # 410 for soft-deleted product re-delete
    │   │   ├── filter/                          # CorrelationIdFilter + HttpRequestLoggingFilter (inbound HTTP)
    │   │   ├── mapper/CatalogMapper.java        # MapStruct entity ↔ DTO mappers
    │   │   ├── messaging/                       # KafkaProducerCorrelationInterceptor (stamps X-Correlation-Id)
    │   │   │   └── contract/CatalogMessageHeaders.java
    │   │   ├── repository/                      # JPA repos + ProductSpecifications (Criteria API, no SQL strings)
    │   │   └── service/                         # ProductService, CategoryService (business rules)
    │   └── resources/
    │       ├── application.yml
    │       ├── logback-spring.xml              # console (plain/JSON), rolling file + Loki appender
    │       └── db/migration/
    │           ├── V1__create_catalog_schema.sql
    │           └── V2__seed_catalog_data.sql    # 24 DummyJSON products / 6 categories
    └── test/java/com/rally/catalog/service/     # ProductServiceTest (28), CategoryServiceTest (7)
```

---

## What Is Implemented (status)

| Area | Status | Notes |
|---|---|---|
| Category CRUD | Done | create/list/get/update/delete; duplicate name → 400; delete-with-products → 400 |
| Product create / update / soft delete | Done | status transitions, ownership checks, validation |
| Product moderation | Done | `PENDING_APPROVAL → APPROVED / REJECTED`, re-approve/re-reject → 400, rejected-resubmit → pending |
| Buyer browse/search | Done | keyword `q` (LIKE on name/description), `categoryId` / `sellerId` / price filters, `sort` (`createdAt`/`basePrice`/`name`), pagination |
| Role-based visibility | Done | buyer sees only APPROVED + non-deleted; owner/admin see everything; non-owner `GET /products/{id}` → 404 |
| `POST /products/lookup` | Done | used by Order Service checkout; `found`/`notFound` split; empty/>50 → 400 |
| API Gateway integration | Done | `rally-gateway` validates JWTs and injects `X-User-Id` / `X-User-Role` / `X-User-Name`; role/ownership enforced service-side — see [Identity & Gateway](#identity--api-gateway-contract) |
| Observability & structured logging | Done | correlation-id + HTTP request logging filters, Kafka producer correlation, outbound REST correlation, Micrometer OTel tracing, Logstash/Loki appenders, Prometheus endpoint (see [Logging & Observability](#logging--observability)) |
| Seed data | Done | 24 real demo products across 6 categories (V2 migration) |
| Swagger/OpenAPI | Not added | no springdoc dependency |

---

## API Endpoints

Base URL (local Docker): `http://localhost:8083`

### Categories

| Method | Path | Notes |
|---|---|---|
| `POST` | `/categories` | create (name required, unique → 400) |
| `GET` | `/categories` | list |
| `GET` | `/categories/{id}` | get one (404 if missing) |
| `PATCH` | `/categories/{id}` | partial update |
| `DELETE` | `/categories/{id}` | 400 if category still has products |

### Products

| Method | Path | Notes |
|---|---|---|
| `POST` | `/products` | create → 201 `PENDING_APPROVAL`; `sellerId` from `X-User-Id` header |
| `GET` | `/products` | buyer browse/search: `q`, `categoryId`, `sellerId`, `minPrice`, `maxPrice`, `sort=field:dir`, `page` (1-based), `limit` |
| `GET` | `/products/{id}` | 404 for non-owner buyers; 200 for owner / `ADMIN` |
| `PATCH` | `/products/{id}` | owner-only (403 otherwise); resets status → `PENDING_APPROVAL` |
| `DELETE` | `/products/{id}` | soft delete → 204; re-delete → 410 |
| `POST` | `/products/lookup` | `{ "productIds": [...] }` → `{ found, notFound }` (Order Service) |
| `GET` | `/products/sellers/{sellerId}` | seller's own products; `status`, `includeDeleted` filters |
| `GET` | `/products/admin` | admin queue; `status`, `includeDeleted` filters |
| `PATCH` | `/products/admin/{id}/approve` | → 200 `APPROVED`; already-approved → 400 |
| `PATCH` | `/products/admin/{id}/reject` | body `{ "reason": "..." }` → 200 `REJECTED` |

---

## Identity & API Gateway Contract

The API Gateway (`rally-gateway`, running) is the single entry point and owns JWT
handling:

1. Client sends `Authorization: Bearer <JWT>` to the **API Gateway**.
2. Gateway validates the JWT with `rally-security` `JwtService.parseAndValidate`
   (RS256 — signed by the Auth service's RSA private key; the gateway only holds the
   public key) and extracts `sub` (user id), roles, and username.
3. Gateway strips the token, removes any client-supplied identity headers, and
   **injects** its own downstream:
   - `X-User-Id` — JWT `sub`
   - `X-User-Role` — JWT roles (comma-joined)
   - `X-User-Name` — JWT username
4. Catalog never validates tokens itself. `SecurityConfig` is `permitAll`; role/ownership
   rules are enforced service-side from the injected headers:
   - `AdminRoleFilter` (an auto-registered `@Component`) guards `/products/admin/*`
     against non-`ADMIN` callers → 403;
   - ownership checks (non-owner → 403) run in `ProductService`.

The Postman collection (`postman/rally-catalog.postman_collection.json`) simulates the
gateway by setting `X-User-Id` / `X-User-Role` / `X-User-Name` directly.

---

## Logging & Observability

Implemented to match `rally-order` / `rally-payment` (branch `feature/catalog-logging`).

### Correlation IDs

| Direction | Mechanism |
|---|---|
| Inbound HTTP | `filter/CorrelationIdFilter` — reads `X-Correlation-Id` (generates a UUID if absent), puts it in the SLF4J MDC **and** Micrometer baggage, echoes it on the response |
| Outbound REST | `config/rest/CorrelationIdRequestInterceptor` — stamps `X-Correlation-Id` from MDC on every external call (e.g. `DealServiceClient`) and logs call duration |
| Kafka (producer) | `messaging/KafkaProducerCorrelationInterceptor` — a Kafka `ProducerInterceptor` registered on both producer factories that copies the MDC correlation id into each record's `X-Correlation-Id` header |

### Request logging

`filter/HttpRequestLoggingFilter` (lowest precedence) logs `HTTP <METHOD> <uri> completed with status <n> in <ms>` for every request. `/actuator`, swagger/api-docs, and `/uploads` paths are skipped as noisy.

### Distributed tracing

- Micrometer Tracing with the OpenTelemetry bridge — `management.tracing.*`
- W3C `traceparent` is auto-propagated on outbound Kafka records (`KafkaTemplate.setObservationEnabled(true)` + `spring.kafka.template.observation-enabled`) so spans chain across services
- `X-Correlation-Id` is registered as remote baggage so it shows on spans and in the MDC
- Traces exported via OTLP (`management.otlp.tracing.endpoint`, default `http://localhost:4318/v1/traces`) — e.g. Jaeger / OTel collector

### Log backends (`logback-spring.xml`)

| Profile | Console | Rolling file | Loki |
|---|---|---|---|
| local / dev | plain text with `traceId`/`spanId` in the MDC pattern | `logs/catalog-service.log` (Logstash JSON) | yes |
| prod | Logstash JSON | `logs/archived/catalog-service-*.log` (10MB each, 30 days, 1GB cap) | yes |

- Rolling file + Loki use `net.logstash.logback.encoder` and `com.github.loki4j` (batch 200 / 5s)
- Loki pushes to `loki.url`; the `LOKI` appender labels log stream `app = rally-catalog` plus `traceId` / `spanId` / `correlationId` as structured metadata
- Prometheus metrics exposed at `/actuator/prometheus` (`micrometer-registry-prometheus`)

### Configurable levels

`application.yml` pins framework noise (`org.hibernate`, `org.apache.kafka`, `org.springframework.kafka`, `org.springframework`, `org.flywaydb`) to WARN / selected OFF, and routes everything through:

| Var | Default | Purpose |
|---|---|---|
| `LOGGING_LEVEL_ROOT` | `INFO` | root level |
| `LOGGING_LEVEL_RALLY` | `INFO` | `com.rally.catalog` business level |
| `LOKI_URL` | `http://localhost:3100/loki/api/v1/push` | Loki push endpoint |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4318/v1/traces` | OTLP traces endpoint |

---

## Seed Data

`V2__seed_catalog_data.sql` inserts 24 curated products from the free DummyJSON demo
API (`https://dummyjson.com/products`) across 6 categories: Electronics, Watches,
Shoes, Home & Furniture, Beauty & Fragrance, Menswear. All are `APPROVED` so they
appear in buyer browse/search.

- Deterministic ids: categories `cat-seed-*`, products `dummy-<id>`.
- `ON CONFLICT (name) DO NOTHING` + subquery category refs make it safe to run
  against a DB that already has categories.
- `seller_id` for all seed rows is `user-seed-0001`.

---

## Local Development Setup

### Prerequisites

- JDK 21, Docker + Docker Compose

### 1. Start the database (Docker)

```bash
cd rally-catalog
docker compose up -d --build
```

| Container | Host port | Notes |
|-----------|-----------|-------|
| `catalog-db` | `5433` | PostgreSQL, db `catalog_db`, user/pass `postgres/postgres` |

> The `catalog-service` block in `docker-compose.yml` is **commented out** for local
> dev — you run the app from IntelliJ (or `mvn spring-boot:run`) and only the DB comes
> from Docker. To run the app in Docker instead, uncomment that block (it also enables
> the JVM debug port `5005` for remote debugging from IntelliJ).

Flyway runs `V1` (schema) + `V2` (seed) automatically on startup. To start from a
clean DB: `docker compose down -v && docker compose up -d --build`.

### 2. Configuration

`src/main/resources/application.yml`:

```yaml
server:
  port: 8083

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:catalog_db}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa.hibernate.ddl-auto: validate
  flyway.enabled: true

rally:
  jwt:
    secret: ${JWT_SECRET:/Wepykoli0z1033egoGDdLrm3eohJiOJUxT5L/RxgV0=}   # dev default only
```

The `rally.jwt.secret` is required because `rally-common` auto-configures a
`JwtService` bean at startup (it is not used for request auth yet). In production,
override via the `JWT_SECRET` env var — same value on every service.

Observability is configured via `management.tracing.*`, `management.otlp.tracing.*`,
`loki.url`, and `logging.level.*` (see [Logging & Observability](#logging--observability)).
The `loki.url` default targets the Loki instance shipped by `rally-infrastructure`
(`docker/local/docker-compose.yml`).

### 3. Run the app locally (IntelliJ or Maven)

```bash
mvn spring-boot:run        # or ./mvnw spring-boot:run
```

Requires the `catalog-db` Postgres container from step 1 (reachable at the datasource
URL above). Run from IntelliJ with a `Remote JVM Debug` config on port `5005` to
breakpoint the containerized app, or just run the app config directly.

### 4. Test with Postman

Import `postman/rally-catalog.postman_collection.json`. It auto-saves generated
`categoryId` / `productId` so requests run end-to-end; headers simulate the gateway.

### 5. Run unit tests

```bash
mvn test        # or ./mvnw test   — 35 tests: ProductServiceTest + CategoryServiceTest
```

---

## Known Gaps / What It Needs to Fully Work

From `gaps-and-solutions.md` and a spec-vs-implementation review of
`catalog-service.md` (spec was updated to match the implementation — paths and response
envelopes in the doc reflect the code):

1. **Auth service / API Gateway not built** — no real JWT login flow; without it, anyone
   can set `X-User-Id` / `X-User-Role`. Build auth + gateway, or enable the
   `JwtAuthenticationFilter`.
2. **Admin endpoints not yet role-protected** — `AdminRoleFilter` exists but is
   **disabled** (commented `@Component` / bean in `SecurityConfig`). Uncomment it once
   the Auth service makes `X-User-Role` trustworthy.
3. **Deal-service client contract** — `DELETE /products/{id}` checks the "tied to an
   active deal" rule (409 via `dealServiceClient.hasActiveDeal`). The real client
   (`DealServiceClientImpl`, `@Profile("prod")`) calls
   `GET /internal/deals/product/{productId}/has-active-deals` and maps
   `{ "hasActiveDeals": true|false }` — aligned with Deal Service's
   `InternalDealController.hasActiveDeals`. Outside the `prod` profile the mock
   (`deal.service.mock.has-active-deal`) answers instead. Contract covered by
   `DealServiceClientImplTest`.
4. **Schema deviation** — `id`/`seller_id`/`category_id` use `VARCHAR(36)` (String
   ids with `GenerationType.UUID`) instead of the native `uuid` type in the spec's
   SQL. Invisible at the API level.
3. **Search is LIKE-based, not Postgres full-text** — all queries are built with
   the JPA Criteria API (`ProductSpecifications`), so `q` matches `name`/`description`
   via `LIKE` instead of `to_tsvector` (documented as the agreed behavior in
   `catalog-service.md` §7.1). The `idx_products_fts` GIN index in V1 is unused; swap
   to a registered Hibernate FTS function later if ranking matters.
