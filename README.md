# n11 Bootcamp

> A full e-commerce platform developed as the N11 Bootcamp graduation project, including production-ready design patterns.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.13-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Services](#services)
- [Design Patterns](#design-patterns)
  - [Saga Pattern](#saga-pattern)
  - [CQRS](#cqrs)
  - [Outbox Pattern](#outbox-pattern)
  - [Circuit Breaker](#circuit-breaker)
- [Technology Stack](#technology-stack)
- [Infrastructure](#infrastructure)
- [API Gateway](#api-gateway)
- [Authentication — Keycloak](#authentication--keycloak)
- [Database Migration — Flyway](#database-migration--flyway)
- [Logging — Logstash & ELK](#logging--logstash--elk)
- [TDD](#tdd)
- [CI/CD — GitHub Actions](#cicd--github-actions)
- [Container Build — Jib](#container-build--jib)
- [Setup](#setup)

---

## Overview

ShopEase is an event-driven e-commerce platform that includes product listing, cart management, order processing, and payment flows. All services can be deployed independently, have separate databases, and communicate asynchronously through RabbitMQ.

**Core Features:**

- Role-based authentication (USER / SELLER) — Keycloak OAuth2 / JWT
- Full-text product search on Elasticsearch
- Cart persistence and idempotency control with Redis
- Distributed transaction management with choreography-based Saga over RabbitMQ
- Guaranteed message delivery with Transactional Outbox
- Fault isolation with Resilience4j Circuit Breaker
- Versioned database migration with Flyway
- Centralized logging with Logstash integration
- Docker image build without Dockerfile using Jib

---

## Architecture

```
                          ┌─────────────────────────────────────┐
   Browser / Mobile       │           React Frontend             │
                          │         (Vite · Tailwind CSS)        │
                          └──────────────────┬──────────────────┘
                                             │ HTTP
                          ┌──────────────────▼──────────────────┐
                          │            API Gateway               │
                          │   Spring Cloud Gateway · Port 8081   │
                          │  JWT Validation · Circuit Breaker    │
                          │  Rate Limiter · Request Logging       │
                          └──┬──────┬──────┬──────┬──────┬──────┘
                             │      │      │      │      │
              ┌──────────────▼─┐ ┌──▼───┐ │  ┌───▼──┐ ┌▼──────────────┐
              │  User Service  │ │Prod. │ │  │Invent│ │  Cart Service  │
              │   Port 8087    │ │ Svc  │ │  │ Svc  │ │   Port 8084    │
              │  PostgreSQL    │ │ 8082 │ │  │ 8083 │ │     Redis      │
              └────────────────┘ └──────┘ │  └──────┘ └───────────────┘
                                          │
                          ┌───────────────▼──────────────────────┐
                          │            Order Service              │
                          │            Port 8085                  │
                          │           PostgreSQL                  │
                          └───────────┬──────────────────────────┘
                                      │
                    ┌─────────────────▼──────────────────┐
                    │              RabbitMQ                │
                    │         (Event Bus · AMQP)           │
                    └──────┬──────────────────────┬───────┘
                           │                      │
              ┌────────────▼─────┐    ┌───────────▼──────────┐
              │ Payment Service  │    │ Notification Service  │
              │   Port 8086      │    │     Port 8088         │
              │  PostgreSQL      │    │   Redis · SMTP        │
              └──────────────────┘    └──────────────────────┘
```

---

## Services

| Service                  | Port | Database                   | Responsibility                                              |
| ------------------------ | ---- | -------------------------- | ----------------------------------------------------------- |
| **api-gateway**          | 8081 | Redis                      | Routing, JWT validation, Circuit Breaker, Rate Limiting     |
| **product-service**      | 8082 | PostgreSQL + Elasticsearch | Product CRUD (Write: PG · Read: ES)                         |
| **inventory-service**    | 8083 | PostgreSQL                 | Stock management, reservation, release                      |
| **cart-service**         | 8084 | Redis                      | Cart CRUD, stock check (TTL: 30 min)                        |
| **order-service**        | 8085 | PostgreSQL                 | Order creation, Saga coordination                            |
| **payment-service**      | 8086 | PostgreSQL                 | Payment processing, card validation                          |
| **user-service**         | 8087 | PostgreSQL                 | Registration, login, Keycloak integration                    |
| **notification-service** | 8088 | Redis (idempotency)        | Email notifications — buyer & seller                        |

---

## Design Patterns

### Saga Pattern

The order creation flow is managed with **choreography-based Saga** without a central orchestrator. Each service publishes its own domain event, and other services react by listening to these events.

#### Successful Order Flow

```
User              Order Svc          Inventory Svc       Payment Svc      Notification Svc
   │                   │                    │                   │                  │
   │── POST /orders ──►│                    │                   │                  │
   │                   │ Save(PENDING)       │                   │                  │
   │                   │ ─────────────────► OrderCreatedEvent   │                  │
   │                   │                    │                   │                  │
   │                   │           Reserve Stock                │                  │
   │                   │◄── StockReservedEvent ─────────────────│                  │
   │                   │                    │                   │                  │
   │                   │ STOCK_RESERVED      │                   │                  │
   │                   │ ──────────────────────────────────────►│                  │
   │                   │                  PaymentRequestedEvent  │                  │
   │                   │                    │           Process Payment             │
   │                   │◄─────────────────────── PaymentCompletedEvent ────────────│
   │                   │                    │                   │                  │
   │                   │ CONFIRMED          │                   │                  │
   │                   │ ──────────────────────────────────────────────────────────►
   │                   │                   OrderConfirmedEvent  │        Send Email │
   │◄─ 201 Created ────│                    │                   │                  │
```

#### Failed Flows (Compensating Transactions)

```
Stock Reservation Failure:
  OrderCreatedEvent → Inventory Svc [insufficient stock]
                    → StockReservationFailedEvent
                    → Order Svc: status = CANCELLED ✗

Payment Failure:
  PaymentRequestedEvent → Payment Svc [card declined]
                        → PaymentFailedEvent
                        → Order Svc: status = CANCELLED ✗
                        → StockReleasedEvent → Inventory Svc (reservation is rolled back)
```

#### RabbitMQ Exchange & Queue Structure

```
Exchanges:
  order.events     → order.stock.reserved.queue
                   → order.stock.reservation.failed.queue
                   → order.payment.completed.queue
                   → order.payment.failed.queue

  inventory.events → cart.stock.updated.queue

  payment.events   → notification.order.confirmed.queue

Routing Keys:
  order.created · stock.reserved · stock.reservation.failed
  payment.completed · payment.failed · stock.released
  payment.requested · order.confirmed · stock.updated
```

---

### CQRS

**Product Service** fully separates read and write models:

```
Write Side (Command)                    Read Side (Query)
─────────────────────                   ─────────────────────
ProductCommandService                   ProductQueryService
       │                                        │
       ▼                                        ▼
  PostgreSQL                            Elasticsearch
  (source of truth)                     (search & listing)
       │
       │ OutboxEvent
       ▼
 ElasticsearchOutboxHandler
       │ (async synchronization)
       ▼
  Elasticsearch Index
```

- **Create / Update / Delete** → Written to PostgreSQL, then an OutboxEvent is created
- **Search / List / Filter** → Read from Elasticsearch (full-text search, category filter, price range)
- A **fallback** to PostgreSQL exists for products not found in Elasticsearch

---

### Outbox Pattern

Before sending messages to external systems, each service writes to the `outbox_events` table in the same DB transaction. A separate `OutboxProcessorService` polls these records and publishes them to RabbitMQ. This guarantees **at-least-once delivery** and **data consistency**.

```
Service Method (Transactional)
  ├─ save to domain_table
  └─ save to outbox_events    ← same transaction

OutboxProcessorService (Scheduled)
  └─ fetch records with status=PENDING
     └─ publish to RabbitMQ
        └─ update status=PUBLISHED
```

Each service manages its own `outbox_events` table:

- `product-service` → Elasticsearch sync events
- `inventory-service` → StockReserved / StockReleased events
- `order-service` → OrderCreated / PaymentRequested / OrderConfirmed events
- `payment-service` → PaymentCompleted / PaymentFailed events

---

### Circuit Breaker

API Gateway applies **Circuit Breaker** to each downstream service using Resilience4j.

```
Request Flow:
  Client → Gateway → [Circuit Breaker] → Downstream Service

State Machine:
  CLOSED ──(error rate > threshold)──► OPEN ──(wait 10s)──► HALF-OPEN
    ▲                                                         │
    └──────────────(test requests successful)─────────────────┘
                                  │
                         (test requests fail)
                                  │
                                  ▼
                                OPEN
```

| Parameter                | Default   | Payment |
| ------------------------ | --------- | ------- |
| Sliding Window           | 10 requests | 5 requests |
| Error Threshold          | 50%       | 30%     |
| Open → Half-Open Wait    | 10s       | 30s     |
| Half-Open Test Requests  | 3         | 2       |
| Timeout                  | 10s       | 15s     |

In failure cases, requests are redirected to `/fallback/service-unavailable`, and a meaningful error message is returned to the user.

---

## Technology Stack

### Backend

| Category          | Technology                                        |
| ----------------- | ------------------------------------------------- |
| Framework         | Spring Boot 3.5.13, Spring Cloud 2025.0.0         |
| Language          | Java 21                                           |
| Build             | Gradle (multi-module)                             |
| Gateway           | Spring Cloud Gateway (WebFlux)                    |
| Security          | Spring Security, OAuth2 Resource Server, Keycloak |
| Messaging         | Spring AMQP (RabbitMQ)                            |
| Cache             | Spring Data Redis (Lettuce)                       |
| Search            | Spring Data Elasticsearch 8.12                    |
| Database          | Spring Data JPA, PostgreSQL                       |
| Migration         | Flyway                                            |
| Resilience        | Resilience4j (Circuit Breaker, Time Limiter)      |
| Logging           | Logback, Logstash Encoder 7.4                     |
| Container         | Jib 3.4.4                                         |
| Test              | JUnit 5, Testcontainers, WireMock                 |
| API Documentation | SpringDoc OpenAPI 2.8.5                           |

### Frontend

| Category  | Technology           |
| --------- | -------------------- |
| Framework | React 18 (Vite)      |
| Styling   | Tailwind CSS         |
| HTTP      | Fetch API (no axios) |
| Routing   | React Router v6      |
| Form      | React Hook Form      |

### Infrastructure

| Component     | Version             |
| ------------- | ------------------- |
| PostgreSQL    | 16-alpine           |
| Redis         | 7-alpine            |
| RabbitMQ      | 3-management-alpine |
| Elasticsearch | 8.12.0              |
| Keycloak      | 24.0.0              |

---

## Infrastructure

### Redis Usage Areas

- **Cart Service**: Cart data (Hash structure, 30 min TTL)
- **API Gateway**: Rate Limiter token bucket state
- **Notification Service**: Idempotency control (7-day TTL, prevents duplicate emails)

### RabbitMQ

- All domain events are sent through `topic` exchange
- Consumers use manual acknowledge
- Publisher retry: initial 1s, max 3 attempts
- Consumer retry: exponential backoff (1s → 2s → 4s)

---

## API Gateway

Runs on port `8081`. All requests pass through this service.

**Responsibilities:**

1. **JWT Validation** — validates token signatures via Keycloak JWK Set URI
2. **Authorization** — role-based access control (USER / SELLER / public)
3. **Header Injection** — adds `X-User-ID`, `X-User-Email`, `X-User-Roles` headers to downstream services
4. **Circuit Breaker** — separate Resilience4j configuration for each service
5. **CORS** — allows all origins (development environment)
6. **Logging** — structured logs with correlation ID for each request/response

**Route Structure:**

```
/api/v1/users/**        → user-service:8087
/api/v1/products/**     → product-service:8082
/api/v1/inventories/**  → inventory-service:8083
/api/v1/cart/**         → cart-service:8084
/api/v1/orders/**       → order-service:8085
/api/v1/payments/**     → payment-service:8086
/api/v1/notifications/**→ notification-service:8088
```

**Public Endpoints (no token required):**

```
POST /api/v1/users/register
POST /api/v1/users/login
POST /api/v1/users/refresh
GET  /api/v1/products/**
GET  /api/v1/inventories/**
GET  /actuator/health/**
```

---

## Authentication — Keycloak

Keycloak 24 is used as the OAuth2 / OpenID Connect identity provider.

- **Realm**: `n11Ecommerce`
- **Client**: `ecommerce-backend` (Direct Access Grants + Service Accounts)
- **Roles**: `USER`, `SELLER`
- **Password Policy**: min 8 characters
- **Token Flow**:
  - Access token → kept in memory (`tokenStore`)
  - Refresh token → `HttpOnly` cookie (`path: /api/v1/users`, 30 days)
- **JWT Claims**: `realm_access.roles` → converted to `ROLE_USER` / `ROLE_SELLER` in Gateway

```
User → POST /api/v1/users/login → User Service → Keycloak
                                                          │
                                              access_token + refresh_token
                                                          │
                                        access_token → response body
                                        refresh_token → HttpOnly cookie
```

---

## Database Migration — Flyway

Each service manages its own PostgreSQL database with its own Flyway migrations. In `validate` mode, the service does not start if there is a schema mismatch.

```
product-service/db/migration/
  V1__create_products_table.sql
  V2__create_outbox_events_table.sql
  V3__replace_unique_name_with_composite.sql
  V4__add_seller_id_to_products.sql

inventory-service/db/migration/
  V1__create_inventories_table.sql
  V2__create_outbox_events_table.sql
  V3__add_column_to_inventory_table.sql
  V4__add_last_confirmed_order_id.sql

order-service/db/migration/
  V1__create_orders_table.sql
  V2__create_order_items_table.sql
  V3__create_outbox_events_table.sql
  V4__add_column_to_inventory_table.sql
  V5__add_seller_id_to_order_items.sql

payment-service/db/migration/
  V1__create_payments_table.sql
  V2__create_outbox_events_table.sql
```

---

## Logging — Logstash & ELK

Each Java service produces structured JSON logs using **Logstash Logback Encoder**.

```
Service → logback-spring.xml → LogstashTcpSocketAppender → Logstash:5044
                             → ConsoleAppender (stdout)
```

**Log Structure:**

```json
{
  "@timestamp": "2026-05-03T16:22:57.123Z",
  "level": "INFO",
  "logger": "o.n11bootcamp.orderservice.services.impl.OrderServiceImpl",
  "message": "Order created. orderId=abc123, userId=xyz, totalAmount=1500.00",
  "service": "order-service",
  "correlationId": "e41af4e3-bd2c-4069-9dae"
}
```

API Gateway adds `correlationId` to every request; this ID is propagated to all downstream service logs and enables distributed tracing.

> If Logstash server (`localhost:5044`) is not running, services continue to write to stdout; the application is not affected.

---

## TDD

The project was developed with **Test-Driven Development** principles. For each feature, tests were written first, then implementation was completed.

**Test Coverage:**

| Service           | Test Classes                                                                                                                                           |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| product-service   | `ProductCommandServiceTests`, `ProductQueryServiceTest`, `ProductSearchRepositoryTest`, `ElasticsearchOutboxHandlerTest`, `OutboxProcessorServiceTest` |
| api-gateway       | `ApiGatewayIntegrationTest`, `FallbackControllerTest`, `SecurityConfigTest`, `LoggingFilterTest`, `CorrelationIdFilterTest`                            |
| inventory-service | `InventoryServiceApplicationTests`                                                                                                                     |
| cart-service      | `CartServiceApplicationTests`                                                                                                                          |
| order-service     | `OrderServiceApplicationTests`                                                                                                                         |
| payment-service   | `PaymentServiceApplicationTests`                                                                                                                       |
| user-service      | `UserServiceApplicationTests`                                                                                                                          |

**Testing Tools:**

- **JUnit 5** — unit and integration tests
- **Testcontainers** — integration tests with real PostgreSQL, Elasticsearch, Redis containers
- **WireMock** — mocking Feign clients
- **Spring Security Test** — JWT and role-based endpoint tests

---

## CI/CD — GitHub Actions

`.github/workflows/ci.yml` includes a two-stage pipeline:

### 1. Build & Test (every push/PR)

```
push/PR → Checkout → JDK 21 setup → Gradle cache → ./gradlew test --parallel
                                                               │
                                                    Test reports uploaded as artifacts
```

### 2. Jib Build & Push (main branch only)

```
main push → [after Build & Test passes]
          → Docker Hub login
          → ./gradlew jib --parallel
          → All service images are pushed to Docker Hub
```

**Required Secrets:**

```
DOCKER_USERNAME  → Docker Hub username
DOCKER_PASSWORD  → Docker Hub access token
```

---

## Container Build — Jib

Docker image build process with [Google Jib](https://github.com/GoogleContainerTools/jib) **without requiring a Dockerfile**:

**Advantages:**

- The whole project is not copied — only changed layers are rebuilt
- Dependencies and application code are in separate layers → when dependencies do not change, build finishes in seconds
- No Docker daemon required (works smoothly in CI environments)
- Reproducible builds

**Usage:**

```bash
# Build all services to local Docker daemon
./gradlew jibDockerBuild --parallel

# Single service
./gradlew :cart-service:jibDockerBuild
./gradlew :order-service:jibDockerBuild

# Push to registry (CI/CD)
./gradlew jib --parallel
```

**Image Configuration** (root `build.gradle`):

```
Base Image : eclipse-temurin:21-jre-alpine
Image Name : ecommerce/{service-name}:latest
JVM Flags  : -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

---

## Setup

### Requirements

- Docker & Docker Compose
- JDK 21 (only for local build)

### 1. Build Images

```bash
# Build all Java services with Jib
./gradlew jibDockerBuild --parallel
```

### 2. Start Services

```bash
docker compose up -d
```

### 3. Wait for Services to Become Healthy

```bash
docker compose ps
```

It can take around 30 seconds for Keycloak to become `healthy`.

### 4. Access the Application

| Component           | URL                                  |
| ------------------- | ------------------------------------ |
| Frontend            | http://localhost:5173                |
| API Gateway         | http://localhost:8081                |
| Keycloak Admin      | http://localhost:8080 (admin/admin)  |
| RabbitMQ Management | http://localhost:15672 (guest/guest) |

### Update a Single Service During Development

```bash
# 1. Change code
# 2. Rebuild only the related service
./gradlew :order-service:jibDockerBuild

# 3. Restart
docker compose up -d order-service
```

### Monitor Logs

```bash
# All services
docker compose logs -f

# Single service
docker compose logs -f order-service

# Error logs only
docker compose logs order-service 2>&1 | grep ERROR
```

---

## Project Structure

```
e-commerce/
├── .github/
│   └── workflows/
│       └── ci.yml              # GitHub Actions CI/CD pipeline
├── api-gateway/                # Spring Cloud Gateway — Port 8081
├── product-service/            # Product management (CQRS) — Port 8082
├── inventory-service/          # Stock management — Port 8083
├── cart-service/               # Cart (Redis) — Port 8084
├── order-service/              # Order + Saga — Port 8085
├── payment-service/            # Payment processing — Port 8086
├── user-service/               # Authentication — Port 8087
├── notification-service/       # Email notifications — Port 8088
├── frontend/                   # React + Vite — Port 5173
├── keycloak/
│   └── realm-export.json       # n11Ecommerce realm configuration
├── build.gradle                # Root build — Jib + Spring Boot + Java
├── settings.gradle             # Multi-module definitions
├── dependencies.gradle         # Central dependency versions
├── docker-compose.yml          # Full infrastructure + services
├── Dockerfile                  # Legacy — Jib is preferred
└── init-db.sql                 # PostgreSQL database creation script
```
