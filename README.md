# ShopEase-Microservices

> A comprehensive e-commerce platform developed as the n11 Bootcamp capstone project, featuring production-ready design patterns.

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
- [Config Server](#config-server)
- [Authentication — Keycloak](#authentication--keycloak)
- [Database Migration — Flyway](#database-migration--flyway)
- [Logging — Logstash & ELK](#logging--logstash--elk)
- [TDD](#tdd)
- [CI/CD — GitHub Actions](#cicd--github-actions)
- [Container Build — Jib](#container-build--jib)
- [Setup](#setup)

---

## Overview

ShopEase is an event-driven e-commerce platform covering product listing, cart management, order processing, and payment flows. All services are independently deployable, have their own dedicated databases, and communicate asynchronously via RabbitMQ.

**Key Features:**

- Role-based authentication (USER / SELLER) — Keycloak OAuth2 / JWT
- Full-text product search powered by Elasticsearch
- Cart persistence and idempotency control via Redis
- Distributed transaction management via choreography-based Saga over RabbitMQ
- Guaranteed message delivery through the Transactional Outbox pattern
- Fault isolation with Resilience4j Circuit Breaker
- Centralized configuration management via Spring Cloud Config Server
- Versioned database migrations with Flyway
- Centralized logging with Logstash integration
- Dockerfile-free Docker image builds with Jib

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

| Service                  | Port | Database                   | Responsibility                                         |
| ------------------------ | ---- | -------------------------- | ------------------------------------------------------ |
| **api-gateway**          | 8081 | Redis                      | Routing, JWT validation, Circuit Breaker, Rate Limiting |
| **product-service**      | 8082 | PostgreSQL + Elasticsearch | Product CRUD (Write: PG · Read: ES)                    |
| **inventory-service**    | 8083 | PostgreSQL                 | Stock management, reservation, release                 |
| **cart-service**         | 8084 | Redis                      | Cart CRUD, stock validation (TTL: 30 min)              |
| **order-service**        | 8085 | PostgreSQL                 | Order creation, Saga coordination                      |
| **payment-service**      | 8086 | PostgreSQL                 | Payment processing, card validation                    |
| **user-service**         | 8087 | PostgreSQL                 | Registration, login, Keycloak integration              |
| **notification-service** | 8088 | Redis (idempotency)        | Email notifications — buyer & seller                   |
| **config-server**        | 8888 | Git Repository             | Centralized configuration distribution                 |

---

## Design Patterns

### Saga Pattern

The order creation flow is managed using a **Choreography-based Saga** without a central orchestrator. Each service publishes its own domain event; other services listen to these events and react accordingly.

#### Successful Order Flow

```
User               Order Svc          Inventory Svc       Payment Svc      Notification Svc
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

#### Failure Flows (Compensating Transactions)

```
Stock Reservation Failure:
  OrderCreatedEvent → Inventory Svc [insufficient stock]
                    → StockReservationFailedEvent
                    → Order Svc: status = CANCELLED ✗

Payment Failure:
  PaymentRequestedEvent → Payment Svc [card declined]
                        → PaymentFailedEvent
                        → Order Svc: status = CANCELLED ✗
                        → StockReleasedEvent → Inventory Svc (reservation rolled back)
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

The **Product Service** fully separates its read and write models:

```
Write Side (Command)                    Read Side (Query)
─────────────────────                   ─────────────────────
ProductCommandService                   ProductQueryService
       │                                        │
       ▼                                        ▼
  PostgreSQL                            Elasticsearch
  (source of truth)                  (search & listing)
       │
       │ OutboxEvent
       ▼
 ElasticsearchOutboxHandler
       │ (async synchronization)
       ▼
  Elasticsearch Index
```

- **Create / Update / Delete** → written to PostgreSQL, an OutboxEvent is created
- **Search / List / Filter** → read from Elasticsearch (full-text search, category filter, price range)
- A **fallback** mechanism to PostgreSQL is in place for products not found in Elasticsearch

---

### Outbox Pattern

Before sending messages to external systems, each service saves them to an `outbox_events` table within the same DB transaction. A separate `OutboxProcessorService` polls these records and delivers them to RabbitMQ, ensuring **at-least-once delivery** and **data consistency**.

```
Service Method (Transactional)
  ├─ write to domain_table
  └─ write to outbox_events    ← same transaction

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

The API Gateway applies a **Circuit Breaker** against each downstream service using Resilience4j.

```
Request Flow:
  Client → Gateway → [Circuit Breaker] → Downstream Service

State Machine:
  CLOSED ──(error rate > threshold)──► OPEN ──(wait 10s)──► HALF-OPEN
    ▲                                                              │
    └──────────────(test requests successful)─────────────────────┘
                                  │
                         (test requests failed)
                                  │
                                  ▼
                                OPEN
```

| Parameter                | Default    | Payment  |
| ------------------------ | ---------- | -------- |
| Sliding Window           | 10 requests | 5 requests |
| Error Threshold          | 50%        | 30%      |
| Open → Half-Open Wait    | 10s        | 30s      |
| Half-Open Test Requests  | 3          | 2        |
| Timeout                  | 10s        | 15s      |

On failure, requests are redirected to the `/fallback/service-unavailable` endpoint, returning a meaningful error message to the client.

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
| Testing           | JUnit 5, Testcontainers, WireMock                 |
| API Documentation | SpringDoc OpenAPI 2.8.5                           |

### Frontend

| Category  | Technology            |
| --------- | --------------------- |
| Framework | React 18 (Vite)       |
| Styling   | Tailwind CSS          |
| HTTP      | Fetch API (no axios)  |
| Routing   | React Router v6       |
| Forms     | React Hook Form       |

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

- All domain events are delivered over `topic` exchanges
- Consumers use manual acknowledgment
- Publisher retry: initial 1s, max 3 attempts
- Consumer retry: exponential backoff (1s → 2s → 4s)

---

## API Gateway

Runs on port `8081`. All requests pass through here.

**Responsibilities:**

1. **JWT Validation** — Validates token signatures via Keycloak JWK Set URI
2. **Authorization** — Role-based access control (USER / SELLER / public)
3. **Header Injection** — Adds `X-User-ID`, `X-User-Email`, `X-User-Roles` headers to downstream requests
4. **Circuit Breaker** — Separate Resilience4j configuration per service
5. **CORS** — Allows all origins (development environment)
6. **Logging** — Structured logs with correlation ID for every request/response

**Route Structure:**

```
/api/v1/users/**         → user-service:8087
/api/v1/products/**      → product-service:8082
/api/v1/inventories/**   → inventory-service:8083
/api/v1/cart/**          → cart-service:8084
/api/v1/orders/**        → order-service:8085
/api/v1/payments/**      → payment-service:8086
/api/v1/notifications/** → notification-service:8088
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

## Config Server

The system uses Spring Cloud Config Server for centralized configuration management. Backend services fetch their configuration from `http://localhost:8888` at startup.

- **Service Name:** `config-server-git`
- **Port:** `8888`
- **Config Source:** Git-based remote repository (defined in `config-server/src/main/resources/application.yml`)

---

## Authentication — Keycloak

Keycloak 24 is used as the OAuth2 / OpenID Connect identity provider.

- **Realm**: `n11Ecommerce`
- **Client**: `ecommerce-backend` (Direct Access Grants + Service Accounts)
- **Roles**: `USER`, `SELLER`
- **Password Policy**: min 8 characters
- **Token Flow**:
  - Access token → stored in memory (`tokenStore`)
  - Refresh token → `HttpOnly` cookie (`path: /api/v1/users`, 30 days)
- **JWT Claims**: `realm_access.roles` → converted to `ROLE_USER` / `ROLE_SELLER` at the Gateway

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

Each service manages its own PostgreSQL database through its own Flyway migrations. With `validate` mode enabled, the service will not start if there is a schema mismatch.

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

Each Java service produces structured JSON logs using the **Logstash Logback Encoder**.

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

The API Gateway appends a `correlationId` to every request; this ID is propagated across all downstream service logs, enabling distributed tracing.

> If the Logstash server (`localhost:5044`) is unavailable, services continue writing to stdout without any impact on the application.

---

## TDD

The project was developed following **Test-Driven Development** principles. Tests were written before each implementation.

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
- **Testcontainers** — integration tests with real PostgreSQL, Elasticsearch, and Redis containers
- **WireMock** — Feign client mocking
- **Spring Security Test** — JWT and role-based endpoint tests

---

## CI/CD — GitHub Actions

`.github/workflows/ci.yml` contains a two-stage pipeline:

### 1. Build & Test (every push/PR)

```
push/PR → Checkout → JDK 21 setup → Gradle cache → ./gradlew test --parallel
                                                              │
                                                   Test reports uploaded as artifact
```

### 2. Jib Build & Push (main branch only)

```
main push → [after Build & Test passes]
          → Docker Hub login
          → ./gradlew jib --parallel
          → All service images pushed to Docker Hub
```

**Required Secrets:**

```
DOCKER_USERNAME  → Docker Hub username
DOCKER_PASSWORD  → Docker Hub access token
```

---

## Container Build — Jib

**Dockerfile-free** Docker image build process using [Google Jib](https://github.com/GoogleContainerTools/jib):

**Advantages:**

- Only changed layers are rebuilt — the entire project is not copied
- Dependencies and application code are in separate layers → fast builds when dependencies haven't changed
- No Docker daemon required (works seamlessly in CI environments)
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
Base Image  : eclipse-temurin:21-jre-alpine
Image Name  : ecommerce/{service-name}:latest
JVM Flags   : -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

---

## Setup

### Requirements

- Docker & Docker Compose
- JDK 21 (for local builds only)

### 1. Build Images

```bash
# Build all Java services with Jib
./gradlew jibDockerBuild --parallel
```

### 2. Start Services

```bash
docker compose up -d
```

### 3. Wait for Services to Be Ready

```bash
docker compose ps
```

Keycloak may take ~30 seconds to reach a `healthy` state.

### 4. Access the Application

| Component           | URL                                  |
| ------------------- | ------------------------------------ |
| Frontend            | http://localhost:5173                |
| API Gateway         | http://localhost:8081                |
| Config Server       | http://localhost:8888                |
| Keycloak Admin      | http://localhost:8080 (admin/admin)  |
| RabbitMQ Management | http://localhost:15672 (guest/guest) |

### Updating a Single Service During Development

```bash
# 1. Modify the code
# 2. Rebuild only the relevant service
./gradlew :order-service:jibDockerBuild

# 3. Restart
docker compose up -d order-service
```

### Monitoring Logs

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
├── config-server/              # Spring Cloud Config Server — Port 8888
├── frontend/                   # React + Vite — Port 5173
├── keycloak/
│   └── realm-export.json       # n11Ecommerce realm configuration
├── build.gradle                # Root build — Jib + Spring Boot + Java
├── settings.gradle             # Multi-module definitions
├── dependencies.gradle         # Centralized dependency versions
├── docker-compose.yml          # All infrastructure + services
├── Dockerfile                  # Legacy — Jib preferred
└── init-db.sql                 # PostgreSQL database creation script
```
