# ParkEase — Backend

> Microservices-based smart parking platform built with Spring Boot 3, deployed on Render.

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?logo=postgresql)](https://www.postgresql.org/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.12-FF6600?logo=rabbitmq)](https://www.rabbitmq.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)](https://www.docker.com/)
[![Render](https://img.shields.io/badge/Deployed_on-Render-46E3B7?logo=render)](https://render.com/)

**Live API:** `https://parkease-api-gateway.onrender.com`  
**Swagger UI:** `https://parkease-api-gateway.onrender.com/swagger-ui.html`  
**Frontend:** [parkease-frontend-zeta.vercel.app](https://parkease-frontend-zeta.vercel.app)

---

## Architecture

ParkEase follows a microservices architecture with an API Gateway as the single entry point. Services communicate asynchronously via RabbitMQ for event-driven workflows and synchronously via REST for direct queries.

```
                        ┌─────────────────────────────────┐
                        │          API Gateway             │
                        │    Spring Cloud Gateway :8080    │
                        │  JWT validation · CORS · Routes  │
                        └──────────────┬──────────────────┘
                                       │
          ┌──────────┬─────────────────┼─────────────┬───────────┐
          │          │                 │             │           │
    ┌─────▼───┐ ┌────▼────┐ ┌─────────▼──┐ ┌───────▼──┐ ┌──────▼───────┐
    │  Auth   │ │ Parking │ │  Booking   │ │ Payment  │ │ Notification │
    │  :8081  │ │  :8082  │ │   :8083    │ │  :8084   │ │    :8085     │
    └─────────┘ └─────────┘ └────────────┘ └──────────┘ └──────────────┘
                                                              ┌───────────┐
                                                              │ Analytics │
                                                              │   :8086   │
                                                              └───────────┘
          └──────────────────── RabbitMQ (Events) ──────────────────────┘
          └──────────────────── PostgreSQL (Data) ───────────────────────┘
          └────────────────────── Redis (Cache) ───────────────────────┘
```

### Services

| Service | Port | Responsibility |
|---|---|---|
| **API Gateway** | 8080 | JWT auth, routing, CORS, circuit breaker |
| **Auth Service** | 8081 | Registration, login, Google OAuth2, user management |
| **Parking Service** | 8082 | Lot/spot CRUD, geo search, Redis availability counters |
| **Booking Service** | 8083 | Booking lifecycle, distributed locking, expiry scheduler |
| **Payment Service** | 8084 | Razorpay integration, payment verification |
| **Notification Service** | 8085 | In-app notifications, transactional emails via Resend |
| **Analytics Service** | 8086 | Occupancy, utilisation, traffic and revenue analytics |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3, Spring Cloud Gateway, Spring Security |
| Databases | PostgreSQL 15 (JPA/Hibernate), Redis 7 |
| Messaging | RabbitMQ 3.12 (TLS in prod) |
| Auth | JWT (JJWT), Google OAuth2 |
| Concurrency | Redisson distributed locks |
| Payments | Razorpay |
| Email | Resend |
| Docs | SpringDoc OpenAPI (aggregated Swagger UI at gateway) |
| DevOps | Docker, Docker Compose, Render |

---

## Key Design Decisions

- **Distributed locking**: Redisson Redis locks prevent double-booking the same spot under concurrent requests
- **Event-driven**: Booking and payment events flow through RabbitMQ — notifications and analytics are fully decoupled from core booking logic
- **Gateway auth**: JWT is validated once at the gateway via `JwtAuthGlobalFilter`; downstream services trust `X-User-Id` and `X-User-Role` headers — no token re-validation per service
- **Circuit breaker**: Analytics route has a Resilience4j circuit breaker with a graceful fallback — analytics being down never breaks the booking flow
- **Role-based access**: `DRIVER`, `MANAGER`, `ADMIN` roles enforced at both gateway (path-level) and service (method-level `@PreAuthorize`)

---

## Running Locally

### Prerequisites

- Docker & Docker Compose
- Java 17 + Maven (only needed to build from source)

### With Docker Compose

```bash
git clone https://github.com/VaidikPandey/ParkEase.git
cd ParkEase

# Copy and fill in env vars
cp .env.example .env

# Start infrastructure + all services
docker-compose -f docker-compose.dev.yml up -d
```

Services will be available at their respective ports. Eureka dashboard at `http://localhost:8761`.

### Environment Variables (local)

| Variable | Description |
|---|---|
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret |
| `RAZORPAY_KEY_ID` | Razorpay API key |
| `RAZORPAY_KEY_SECRET` | Razorpay secret |
| `MAIL_USERNAME` | Gmail address for SMTP |
| `MAIL_PASSWORD` | Gmail App Password |

---

## API Overview

All requests go through the gateway at `https://parkease-api-gateway.onrender.com`. The full interactive docs are at `/swagger-ui.html`.

### Auth
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public |
| GET | `/oauth2/authorization/google` | Public |
| POST | `/api/v1/auth/refresh` | Public |

### Parking
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/v1/parking/lots/search` | Public |
| GET | `/api/v1/parking/lots/nearby` | Public |
| POST | `/api/v1/parking/manager/lots` | MANAGER |
| GET | `/api/v1/parking/admin/lots/pending` | ADMIN |

### Booking
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/bookings` | DRIVER |
| POST | `/api/v1/bookings/{id}/checkin` | DRIVER |
| POST | `/api/v1/bookings/{id}/checkout` | DRIVER |
| DELETE | `/api/v1/bookings/{id}` | DRIVER |

### Payment
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/payments/orders` | DRIVER |
| POST | `/api/v1/payments/verify` | DRIVER |

### Notifications
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/v1/notifications/recipient/{id}` | Authenticated |
| PATCH | `/api/v1/notifications/{id}/read` | Authenticated |

### Analytics
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/v1/analytics/lots/{id}/occupancy` | MANAGER / ADMIN |
| GET | `/api/v1/analytics/lots/{id}/summary` | MANAGER / ADMIN |
| GET | `/api/v1/analytics/my` | DRIVER |

---

## Event Flow

```
DRIVER books spot
      │
      ▼
Booking Service ──► booking.pending ──► Notification Service (in-app + email)
      │
      ▼ (Razorpay payment)
Payment Service ──► payment.completed ──► Booking Service (confirms booking)
                                     └──► Notification Service
                                     └──► Analytics Service
      │
      ▼
Booking Service ──► booking.confirmed ──► Notification Service (receipt email)
                                     └──► Analytics Service
```

---

## Deployment

Services are deployed individually on **Render free tier** using Docker containers. Each service has its own Dockerfile with a multi-stage build (Maven build → JRE runtime).

Production differences from local:
- Eureka disabled (`EUREKA_CLIENT_ENABLED=false`); gateway routes directly to service URLs
- RabbitMQ over TLS (port 5671)
- Redis Cloud with TLS
- All secrets injected via Render environment variables

---

## Author

**Vaidik Pandey**  
[GitHub](https://github.com/VaidikPandey)
