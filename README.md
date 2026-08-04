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
- **rally-common** — shared `BaseException` hierarchy + `JwtService`
- **H2** (test scope) + JUnit 5 / Mockito for unit tests

---

## Project Structure

```
rally-catalog/
├── Dockerfile                    # multi-stage: installs rally-common v0.2.0 from GitHub, packages jar
├── docker-compose.yml            # catalog-db (host 5433) + catalog-service (host 8083)
├── postman/
│   └── rally-catalog.postman_collection.json   # importable end-to-end test collection
└── src/
    ├── main/
    │   ├── java/com/rally/catalog/
    │   │   ├── CatalogServiceApplication.java
    │   │   ├── config/SecurityConfig.java      # permitAll; role checks live in ProductService
    │   │   ├── controller/                      # CategoryController, ProductController
    │   │   ├── dto/                             # request/response records (PageResponse, ...)
    │   │   ├── entity/                          # Category, Product, ProductStatus
    │   │   ├── exception/GoneException.java     # 410 for soft-deleted product re-delete
    │   │   ├── repository/                      # JPA repos + ProductSpecifications (Criteria API, no SQL strings)
    │   │   └── service/                         # ProductService, CategoryService (business rules)
    │   └── resources/
    │       ├── application.yml
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
| Seed data | Done | 24 real demo products across 6 categories (V2 migration) |
| API Gateway | **Not built yet** | required for real auth — see [Identity & Gateway](#identity--api-gateway-contract) |
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

The gateway (not yet built) is the single entry point and owns JWT handling:

1. Client sends `Authorization: Bearer <JWT>` to the **API Gateway**.
2. Gateway validates the JWT (rally-common `JwtService.parseAndValidate`; JWT is
   signed HMAC, not encrypted) and extracts `sub` (user id) + `roles`.
3. Gateway strips the token and injects identity headers downstream:
   - `X-User-Id` — JWT `sub`
   - `X-User-Role` — JWT roles
4. Catalog never validates tokens itself. `SecurityConfig` is `permitAll` and the
   rally-common `JwtAuthenticationFilter` is registered **disabled**; role/ownership
   rules are enforced in `ProductService` from the injected headers.

The Postman collection (`postman/rally-catalog.postman_collection.json`) simulates
the gateway by setting `X-User-Id` / `X-User-Role` directly.

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

### 1. Start the service (recommended)

```bash
cd rally-catalog
docker compose up -d --build
```

| Container       | Host port | Notes |
|-----------------|-----------|-------|
| `catalog-service` | `8083`  | Spring Boot app |
| `catalog-db`      | `5433`  | PostgreSQL, db `catalog_db`, user/pass `postgres/postgres` |

Flyway runs `V1` (schema) + `V2` (seed) automatically on startup. To start from a
clean DB: `docker compose down -v && docker compose up -d --build`.

### 2. Configuration

`src/main/resources/application.yml`:

```yaml
server:
  port: 8083

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:catalog_db}
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

### 3. Run locally (without Docker for the app)

```bash
./mvnw spring-boot:run
```

Requires a Postgres reachable at the datasource URL above (e.g. the `catalog-db`
container).

### 4. Test with Postman

Import `postman/rally-catalog.postman_collection.json`. It auto-saves generated
`categoryId` / `productId` so requests run end-to-end; headers simulate the gateway.

### 5. Run unit tests

```bash
./mvnw test        # 35 tests: ProductServiceTest + CategoryServiceTest
```

---

## Known Gaps / What It Needs to Fully Work

From `gaps-and-solutions.md` and a spec-vs-implementation review of
`catalog-service.md`:

1. **API Gateway not built** — no real JWT login flow; without it, anyone can set
   `X-User-Id` / `X-User-Role`. Build the gateway or enable the
   `JwtAuthenticationFilter`.
2. **Admin endpoints not role-protected** — `GET /products/admin` and the
   approve/reject routes accept any caller. Enforce `ADMIN` at the gateway or in a
   filter.
3. **Response-shape mismatches vs the spec doc**:
   - `GET /products` returns `{ "items": [...] }`; spec says `{ "products": [...] }`.
   - `GET /categories` returns a bare array; spec says `{ "categories": [...] }`.
   - Admin paths are `/products/admin/...`; spec says `/admin/products/...`.
   - Seller list is `GET /products/sellers/{id}`; spec says `GET /sellers/{id}/products`.
4. **`DELETE /products/{id}` → 409 "tied to active deal"** not implemented — needs
   Deal Service coordination; currently only soft-delete (204) + re-delete (410).
5. **Schema deviation** — `id`/`seller_id`/`category_id` use `VARCHAR(36)` (String
   ids with `GenerationType.UUID`) instead of the native `uuid` type in the spec's
   SQL. Invisible at the API level.
6. **Search is LIKE-based, not Postgres full-text** — all queries are built with
   the JPA Criteria API (`ProductSpecifications`), so `q` matches `name`/`description`
   via `LIKE` instead of `to_tsvector`. No raw SQL anywhere in the code. The
   `idx_products_fts` GIN index in V1 is therefore unused; swap to a registered
   Hibernate FTS function later if ranking matters.
