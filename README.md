# ParkEase — Microservices Parking Management System

ParkEase is a scalable, microservices-based platform designed to simplify urban parking. It allows drivers to find and book spots in real-time, while providing parking lot managers and administrators with robust tools for oversight and approval.

## 🚀 Core Features

*   **Smart Search**: Find parking lots by city or GPS-based "nearby" search.
*   **Real-time Availability**: Live spot counting powered by Redis.
*   **Booking Lifecycle**: Full end-to-end booking (Reserve → Check-in → Check-out → Automated Fare Calculation).
*   **Secure Auth**: JWT-based authentication with Google OAuth2 integration.
*   **Admin Dashboard**: Approve new parking lots and manage user accounts.
*   **Event-Driven**: Asynchronous notifications and status updates via RabbitMQ.

## 🛠️ Tech Stack

| Category | Technology |
| :--- | :--- |
| **Backend** | Java 17, Spring Boot 3, Spring Security |
| **Microservices** | Spring Cloud Netflix Eureka (Discovery) |
| **Databases** | PostgreSQL (Primary), Redis (Caching/Counters) |
| **Messaging** | RabbitMQ (Event Streaming) |
| **Concurrency** | Redisson (Distributed Locking for Bookings) |
| **DevOps** | Docker, Docker Compose |

## 🏗️ Architecture

The system is split into several specialized services:
- **Auth Service**: User identity, OAuth2, and Admin management.
- **Parking Service**: Lot/Spot CRUD and Geo-spatial search.
- **Booking Service**: Reservation logic and lifecycle events.
- **Eureka Server**: Service registration and discovery.
- *(Planned)*: API Gateway, Payment Service, and Notification Service.

## 🚦 Getting Started

### Prerequisites
- Docker & Docker Compose
- Java 17 & Maven (for building from source)

### Running with Docker
1. **Clone the repo**:
   ```bash
   git clone https://github.com/VaidikPandey/ParkEase.git
   cd ParkEase
   ```
2. **Build the services**:
   ```bash
   mvn clean package -DskipTests
   ```
3. **Launch the ecosystem**:
   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```
4. **Access the services**:
   - Eureka Dashboard: `http://localhost:8761`
   - Auth Service: `http://localhost:8081`
   - Parking Service: `http://localhost:8082`
   - Booking Service: `http://localhost:8083`

# Author
Vaidik Pandey
