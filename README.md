# n11 Bootcamp

> N11 Bootcamp bitirme projesi olarak geliştirilmiş, production-ready tasarım kalıpları içeren tam kapsamlı bir e-ticaret platformu.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.13-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

---

## İçindekiler

- [Genel Bakış](#genel-bakış)
- [Mimari](#mimari)
- [Servisler](#servisler)
- [Tasarım Kalıpları](#tasarım-kalıpları)
  - [Saga Pattern](#saga-pattern)
  - [CQRS](#cqrs)
  - [Outbox Pattern](#outbox-pattern)
  - [Circuit Breaker](#circuit-breaker)
- [Teknoloji Yığını](#teknoloji-yığını)
- [Altyapı](#altyapı)
- [API Gateway](#api-gateway)
- [Kimlik Doğrulama — Keycloak](#kimlik-doğrulama--keycloak)
- [Veritabanı Migrasyonu — Flyway](#veritabanı-migrasyonu--flyway)
- [Loglama — Logstash & ELK](#loglama--logstash--elk)
- [TDD](#tdd)
- [CI/CD — GitHub Actions](#cicd--github-actions)
- [Container Build — Jib](#container-build--jib)
- [Kurulum](#kurulum)

---

## Genel Bakış

ShopEase; ürün listeleme, sepet yönetimi, sipariş işleme ve ödeme akışlarını kapsayan event-driven bir e-ticaret platformudur. Tüm servisler birbirinden bağımsız deploy edilebilir, ayrı veritabanlarına sahiptir ve RabbitMQ üzerinden asenkron mesajlaşır.

**Temel Özellikler:**

- Rol tabanlı kimlik doğrulama (USER / SELLER) — Keycloak OAuth2 / JWT
- Elasticsearch üzerinde tam metin ürün arama
- Redis ile sepet kalıcılığı ve idempotency kontrolü
- RabbitMQ üzerinden choreography-based Saga ile dağıtık işlem yönetimi
- Transactional Outbox ile guaranteed message delivery
- Resilience4j Circuit Breaker ile hata yalıtımı
- Flyway ile sürümlü veritabanı migrasyonu
- Logstash entegrasyonlu merkezi loglama
- Jib ile Dockerfile gerektirmeyen Docker image build

---

## Mimari

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

## Servisler

| Servis                   | Port | Veritabanı                 | Sorumluluk                                             |
| ------------------------ | ---- | -------------------------- | ------------------------------------------------------ |
| **api-gateway**          | 8081 | Redis                      | Routing, JWT doğrulama, Circuit Breaker, Rate Limiting |
| **product-service**      | 8082 | PostgreSQL + Elasticsearch | Ürün CRUD (Write: PG · Read: ES)                       |
| **inventory-service**    | 8083 | PostgreSQL                 | Stok yönetimi, rezervasyon, serbest bırakma            |
| **cart-service**         | 8084 | Redis                      | Sepet CRUD, stok kontrolü (TTL: 30 dk)                 |
| **order-service**        | 8085 | PostgreSQL                 | Sipariş oluşturma, Saga koordinasyonu                  |
| **payment-service**      | 8086 | PostgreSQL                 | Ödeme işleme, kart doğrulama                           |
| **user-service**         | 8087 | PostgreSQL                 | Kayıt, giriş, Keycloak entegrasyonu                    |
| **notification-service** | 8088 | Redis (idempotency)        | E-posta bildirimleri — alıcı & satıcı                  |

---

## Tasarım Kalıpları

### Saga Pattern

Sipariş oluşturma akışı, merkezi bir orkestratör olmadan **Choreography-based Saga** ile yönetilir. Her servis kendi domain event'ini yayımlar; diğer servisler bu event'leri dinleyerek tepki verir.

#### Başarılı Sipariş Akışı

```
Kullanıcı          Order Svc          Inventory Svc       Payment Svc      Notification Svc
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

#### Başarısız Akışlar (Compensating Transactions)

```
Stok Rezervasyon Hatası:
  OrderCreatedEvent → Inventory Svc [yetersiz stok]
                    → StockReservationFailedEvent
                    → Order Svc: status = CANCELLED ✗

Ödeme Hatası:
  PaymentRequestedEvent → Payment Svc [kart reddedildi]
                        → PaymentFailedEvent
                        → Order Svc: status = CANCELLED ✗
                        → StockReleasedEvent → Inventory Svc (rezervasyon geri alınır)
```

#### RabbitMQ Exchange & Queue Yapısı

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

**Product Service** okuma ve yazma modellerini tamamen ayırır:

```
Write Side (Command)                    Read Side (Query)
─────────────────────                   ─────────────────────
ProductCommandService                   ProductQueryService
       │                                        │
       ▼                                        ▼
  PostgreSQL                            Elasticsearch
  (kaynak gerçek)                    (arama & listeleme)
       │
       │ OutboxEvent
       ▼
 ElasticsearchOutboxHandler
       │ (asenkron senkronizasyon)
       ▼
  Elasticsearch Index
```

- **Create / Update / Delete** → PostgreSQL'e yazılır, OutboxEvent oluşturulur
- **Search / List / Filter** → Elasticsearch'ten okunur (tam metin arama, kategori filtresi, fiyat aralığı)
- Elasticsearch'te bulunmayan ürün için PostgreSQL'e **fallback** mekanizması mevcuttur

---

### Outbox Pattern

Her servis, dış sistemlere mesaj göndermeden önce aynı DB transaction'ı içinde `outbox_events` tablosuna kaydeder. Ayrı bir `OutboxProcessorService` bu kayıtları poll ederek RabbitMQ'ya iletir. Bu sayede **at-least-once delivery** ve **veri tutarlılığı** garanti altına alınır.

```
Service Method (Transactional)
  ├─ domain_table'a kaydet
  └─ outbox_events'e kaydet    ← aynı transaction

OutboxProcessorService (Scheduled)
  └─ status=PENDING kayıtları çek
     └─ RabbitMQ'ya publish et
        └─ status=PUBLISHED güncelle
```

Her servis kendi `outbox_events` tablosunu yönetir:

- `product-service` → Elasticsearch sync event'leri
- `inventory-service` → StockReserved / StockReleased event'leri
- `order-service` → OrderCreated / PaymentRequested / OrderConfirmed event'leri
- `payment-service` → PaymentCompleted / PaymentFailed event'leri

---

### Circuit Breaker

API Gateway, Resilience4j ile her downstream servise karşı **Circuit Breaker** uygular.

```
İstek Akışı:
  Client → Gateway → [Circuit Breaker] → Downstream Service

Durum Makinesi:
  CLOSED ──(hata oranı > eşik)──► OPEN ──(10s bekle)──► HALF-OPEN
    ▲                                                         │
    └──────────────(test istekleri başarılı)──────────────────┘
                                  │
                         (test istekleri başarısız)
                                  │
                                  ▼
                                OPEN
```

| Parametre                | Varsayılan | Payment |
| ------------------------ | ---------- | ------- |
| Sliding Window           | 10 istek   | 5 istek |
| Hata Eşiği               | %50        | %30     |
| Open → Half-Open Süresi  | 10s        | 30s     |
| Half-Open Test İstekleri | 3          | 2       |
| Timeout                  | 10s        | 15s     |

Hata durumunda `/fallback/service-unavailable` endpoint'ine yönlendirilir ve kullanıcıya anlamlı bir hata mesajı döner.

---

## Teknoloji Yığını

### Backend

| Kategori          | Teknoloji                                         |
| ----------------- | ------------------------------------------------- |
| Framework         | Spring Boot 3.5.13, Spring Cloud 2025.0.0         |
| Dil               | Java 21                                           |
| Build             | Gradle (multi-module)                             |
| Gateway           | Spring Cloud Gateway (WebFlux)                    |
| Güvenlik          | Spring Security, OAuth2 Resource Server, Keycloak |
| Mesajlaşma        | Spring AMQP (RabbitMQ)                            |
| Cache             | Spring Data Redis (Lettuce)                       |
| Arama             | Spring Data Elasticsearch 8.12                    |
| Veritabanı        | Spring Data JPA, PostgreSQL                       |
| Migrasyon         | Flyway                                            |
| Resilience        | Resilience4j (Circuit Breaker, Time Limiter)      |
| Loglama           | Logback, Logstash Encoder 7.4                     |
| Container         | Jib 3.4.4                                         |
| Test              | JUnit 5, Testcontainers, WireMock                 |
| API Dokümantasyon | SpringDoc OpenAPI 2.8.5                           |

### Frontend

| Kategori  | Teknoloji             |
| --------- | --------------------- |
| Framework | React 18 (Vite)       |
| Stil      | Tailwind CSS          |
| HTTP      | Fetch API (axios yok) |
| Routing   | React Router v6       |
| Form      | React Hook Form       |

### Altyapı

| Bileşen       | Sürüm               |
| ------------- | ------------------- |
| PostgreSQL    | 16-alpine           |
| Redis         | 7-alpine            |
| RabbitMQ      | 3-management-alpine |
| Elasticsearch | 8.12.0              |
| Keycloak      | 24.0.0              |

---

## Altyapı

### Redis Kullanım Alanları

- **Cart Service**: Sepet verisi (Hash yapısı, 30 dk TTL)
- **API Gateway**: Rate Limiter token bucket state
- **Notification Service**: İdempotency kontrolü (7 günlük TTL, tekrar e-posta gönderimi önleme)

### RabbitMQ

- Tüm domain event'leri `topic` exchange üzerinden iletilir
- Consumer'lar manuel acknowledge kullanır
- Publisher retry: initial 1s, max 3 deneme
- Consumer retry: exponential backoff (1s → 2s → 4s)

---

## API Gateway

`8081` portunda çalışır. Tüm istekler buradan geçer.

**Görevleri:**

1. **JWT Doğrulama** — Keycloak JWK Set URI üzerinden token imzası doğrulama
2. **Yetkilendirme** — Rol tabanlı erişim kontrolü (USER / SELLER / public)
3. **Header Enjeksiyonu** — `X-User-ID`, `X-User-Email`, `X-User-Roles` header'larını downstream servislere ekler
4. **Circuit Breaker** — Her servis için ayrı Resilience4j konfigürasyonu
5. **CORS** — Tüm originlere izin verir (development ortamı)
6. **Loglama** — Her istek/cevap için correlation ID ile yapılandırılmış log

**Route Yapısı:**

```
/api/v1/users/**        → user-service:8087
/api/v1/products/**     → product-service:8082
/api/v1/inventories/**  → inventory-service:8083
/api/v1/cart/**         → cart-service:8084
/api/v1/orders/**       → order-service:8085
/api/v1/payments/**     → payment-service:8086
/api/v1/notifications/**→ notification-service:8088
```

**Açık Endpoint'ler (token gerektirmez):**

```
POST /api/v1/users/register
POST /api/v1/users/login
POST /api/v1/users/refresh
GET  /api/v1/products/**
GET  /api/v1/inventories/**
GET  /actuator/health/**
```

---

## Kimlik Doğrulama — Keycloak

Keycloak 24, OAuth2 / OpenID Connect kimlik sağlayıcısı olarak kullanılır.

- **Realm**: `n11Ecommerce`
- **Client**: `ecommerce-backend` (Direct Access Grants + Service Accounts)
- **Roller**: `USER`, `SELLER`
- **Şifre Politikası**: min 8 karakter
- **Token Akışı**:
  - Access token → bellekte tutulur (`tokenStore`)
  - Refresh token → `HttpOnly` cookie (`path: /api/v1/users`, 30 gün)
- **JWT Claim'leri**: `realm_access.roles` → Gateway'de `ROLE_USER` / `ROLE_SELLER`'a dönüştürülür

```
Kullanıcı → POST /api/v1/users/login → User Service → Keycloak
                                                          │
                                              access_token + refresh_token
                                                          │
                                        access_token → response body
                                        refresh_token → HttpOnly cookie
```

---

## Veritabanı Migrasyonu — Flyway

Her servis kendi PostgreSQL veritabanını kendi Flyway migrasyonlarıyla yönetir. `validate` modu ile şema uyuşmazlıklarında servis başlamaz.

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

## Loglama — Logstash & ELK

Her Java servisi **Logstash Logback Encoder** kullanarak yapılandırılmış JSON log üretir.

```
Servis → logback-spring.xml → LogstashTcpSocketAppender → Logstash:5044
                             → ConsoleAppender (stdout)
```

**Log Yapısı:**

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

API Gateway her isteğe `correlationId` ekler; bu ID tüm downstream servis loglarına yayılarak dağıtık trace imkânı sağlar.

> Logstash sunucusu (`localhost:5044`) çalışmıyorsa servisler stdout'a yazmaya devam eder; uygulama etkilenmez.

---

## TDD

Proje **Test-Driven Development** prensibiyle geliştirilmiştir. Her özellik için önce test yazılmış, ardından implementasyon yapılmıştır.

**Test Kapsamı:**

| Servis            | Test Sınıfları                                                                                                                                         |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| product-service   | `ProductCommandServiceTests`, `ProductQueryServiceTest`, `ProductSearchRepositoryTest`, `ElasticsearchOutboxHandlerTest`, `OutboxProcessorServiceTest` |
| api-gateway       | `ApiGatewayIntegrationTest`, `FallbackControllerTest`, `SecurityConfigTest`, `LoggingFilterTest`, `CorrelationIdFilterTest`                            |
| inventory-service | `InventoryServiceApplicationTests`                                                                                                                     |
| cart-service      | `CartServiceApplicationTests`                                                                                                                          |
| order-service     | `OrderServiceApplicationTests`                                                                                                                         |
| payment-service   | `PaymentServiceApplicationTests`                                                                                                                       |
| user-service      | `UserServiceApplicationTests`                                                                                                                          |

**Test Araçları:**

- **JUnit 5** — birim ve entegrasyon testleri
- **Testcontainers** — gerçek PostgreSQL, Elasticsearch, Redis container'ları ile entegrasyon testi
- **WireMock** — Feign client mock'lama
- **Spring Security Test** — JWT ve rol tabanlı endpoint testleri

---

## CI/CD — GitHub Actions

`.github/workflows/ci.yml` iki aşamalı pipeline içerir:

### 1. Build & Test (her push/PR)

```
push/PR → Checkout → JDK 21 kurulum → Gradle cache → ./gradlew test --parallel
                                                               │
                                                    Test raporları artifact olarak yüklenir
```

### 2. Jib Build & Push (sadece main branch)

```
main push → [Build & Test geçtikten sonra]
          → Docker Hub login
          → ./gradlew jib --parallel
          → Tüm servis image'ları Docker Hub'a push edilir
```

**Gerekli Secrets:**

```
DOCKER_USERNAME  → Docker Hub kullanıcı adı
DOCKER_PASSWORD  → Docker Hub access token
```

---

## Container Build — Jib

[Google Jib](https://github.com/GoogleContainerTools/jib) ile **Dockerfile gerektirmeyen** Docker image build süreci:

**Avantajları:**

- Tüm proje kopyalanmaz — sadece değişen layer rebuild edilir
- Dependencies ve uygulama kodu ayrı layer'larda → bağımlılık değişmediğinde saniyeler içinde build
- Docker daemon gerektirmez (CI ortamında sorunsuz çalışır)
- Tekrarlanabilir build'ler

**Kullanım:**

```bash
# Tüm servisleri local Docker daemon'a build et
./gradlew jibDockerBuild --parallel

# Tek servis
./gradlew :cart-service:jibDockerBuild
./gradlew :order-service:jibDockerBuild

# Registry'ye push (CI/CD)
./gradlew jib --parallel
```

**Image Yapılandırması** (root `build.gradle`):

```
Base Image : eclipse-temurin:21-jre-alpine
Image Adı  : ecommerce/{servis-adı}:latest
JVM Flags  : -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

---

## Kurulum

### Gereksinimler

- Docker & Docker Compose
- JDK 21 (sadece local build için)

### 1. Image'ları Build Et

```bash
# Jib ile tüm Java servislerini build et
./gradlew jibDockerBuild --parallel
```

### 2. Servisleri Başlat

```bash
docker compose up -d
```

### 3. Servislerin Ayağa Kalkmasını Bekle

```bash
docker compose ps
```

Keycloak'ın `healthy` durumuna geçmesi ~30 saniye sürebilir.

### 4. Uygulamaya Eriş

| Bileşen             | URL                                  |
| ------------------- | ------------------------------------ |
| Frontend            | http://localhost:5173                |
| API Gateway         | http://localhost:8081                |
| Keycloak Admin      | http://localhost:8080 (admin/admin)  |
| RabbitMQ Management | http://localhost:15672 (guest/guest) |

### Geliştirme Sırasında Tek Servis Güncelleme

```bash
# 1. Kodu değiştir
# 2. Sadece ilgili servisi rebuild et
./gradlew :order-service:jibDockerBuild

# 3. Yeniden başlat
docker compose up -d order-service
```

### Logları İzleme

```bash
# Tüm servisler
docker compose logs -f

# Tek servis
docker compose logs -f order-service

# Sadece hata logları
docker compose logs order-service 2>&1 | grep ERROR
```

---

## Proje Yapısı

```
e-commerce/
├── .github/
│   └── workflows/
│       └── ci.yml              # GitHub Actions CI/CD pipeline
├── api-gateway/                # Spring Cloud Gateway — Port 8081
├── product-service/            # Ürün yönetimi (CQRS) — Port 8082
├── inventory-service/          # Stok yönetimi — Port 8083
├── cart-service/               # Sepet (Redis) — Port 8084
├── order-service/              # Sipariş + Saga — Port 8085
├── payment-service/            # Ödeme işleme — Port 8086
├── user-service/               # Kimlik doğrulama — Port 8087
├── notification-service/       # E-posta bildirimleri — Port 8088
├── frontend/                   # React + Vite — Port 5173
├── keycloak/
│   └── realm-export.json       # n11Ecommerce realm konfigürasyonu
├── build.gradle                # Root build — Jib + Spring Boot + Java
├── settings.gradle             # Multi-module tanımları
├── dependencies.gradle         # Merkezi bağımlılık versiyonları
├── docker-compose.yml          # Tüm altyapı + servisler
├── Dockerfile                  # Legacy — Jib tercih edilir
└── init-db.sql                 # PostgreSQL veritabanı oluşturma scripti
```
