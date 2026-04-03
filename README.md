# RestroMind — Restaurant Management Platform

> **Internship Project**
> **Company:** GravatonWeb Technology
> **Role:** Full-Stack Developer Intern
> **Project:** RestroMind — Full-Stack Restaurant Management Platform
> **Tech Stack:** Java 23 · Spring Boot 3.3 · PostgreSQL · JWT · React Native · Maven · Flyway · Spring Cloud Gateway

---

## Overview

RestroMind is a full-stack restaurant management platform built on a Java Spring Boot microservices backend and a React Native mobile frontend. It serves two types of users from a single app — restaurant owners (Admins) who manage their restaurant, menu, and orders, and customers (Users) who browse restaurants, place orders, and track deliveries.

---

## Tech Stack

### Backend
| Technology | Purpose |
|---|---|
| Java 23 | Primary language |
| Spring Boot 3.3 | Microservices framework |
| Spring Security | Authentication & authorization |
| Spring Data JPA | ORM / database access |
| PostgreSQL | Primary database |
| Flyway | Database schema migrations |
| JJWT 0.12 | JWT token generation & validation |
| BCrypt (cost 12) | Password hashing |
| springdoc-openapi | Swagger UI / API docs |
| Maven 3.9 | Build tool |

### Frontend
| Technology | Purpose |
|---|---|
| React Native | Cross-platform mobile app |
| Expo | Development & build tooling |

### Architecture
| Component | Purpose |
|---|---|
| API Gateway (port 8080) | JWT validation, routing, rate limiting |
| Auth Service (port 8081) | Registration, login, token management |
| Restaurant Service (port 8082) | Restaurant profiles & onboarding |
| Menu Service (port 8083) | Categories & dish management |
| Order Service (port 8084) | Order lifecycle & checkout |
| User Service (port 8085) | Customer profiles & addresses |
| Notification Service (port 8086) | Push, email & in-app notifications |
| Analytics Service (port 8087) | Revenue reports & dashboard metrics |

---

## Project Structure

```
RestroMind/
├── restromind-backend/          # Spring Boot microservices
│   ├── common/                  # Shared DTOs, JwtUtil
│   ├── api-gateway/             # Spring Cloud Gateway
│   ├── auth-service/            # Auth & JWT
│   ├── restaurant-service/      # Restaurant management
│   ├── menu-service/            # Menu & dishes
│   ├── order-service/           # Orders & checkout
│   ├── user-service/            # User profiles
│   ├── notification-service/    # Notifications
│   └── analytics-service/       # Analytics & reports
├── RestroMind-Admin/            # Admin UI designs
└── RestroMind-User/             # User UI designs
```

---

## Services Status

| Service | Status |
|---|---|
| Auth Service | ✅ Complete |
| Restaurant Service | 🔄 In Progress |
| Menu Service | ⏳ Pending |
| Order Service | ⏳ Pending |
| User Service | ⏳ Pending |
| Notification Service | ⏳ Pending |
| Analytics Service | ⏳ Pending |
| API Gateway | ⏳ Pending |

---

## Auth Service APIs

Base URL: `http://localhost:8081`
Swagger UI: `http://localhost:8081/swagger-ui.html`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/register` | Register new Admin or User |
| POST | `/auth/login` | Login and get JWT tokens |
| POST | `/auth/refresh` | Refresh access token |
| POST | `/auth/logout` | Logout and revoke refresh token |

---

## Getting Started

### Prerequisites
- Java 23
- Maven 3.9+
- PostgreSQL 15+

### Setup

1. Clone the repo
```bash
git clone https://github.com/Pratikshapandey1609/RestroMind.git
cd RestroMind
```

2. Create the database
```sql
CREATE DATABASE "RestroMind";
```

3. Set JAVA_HOME to Java 23
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-23"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
```

4. Build all modules
```bash
cd restromind-backend
mvn clean install -DskipTests
```

5. Run Auth Service
```bash
mvn spring-boot:run -pl auth-service
```

---

## Environment Configuration

Each service has its own `application.yml`. Key values to update for production:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/RestroMind
    username: your_db_user
    password: your_db_password

jwt:
  secret: your-secret-key-min-32-chars   # CHANGE THIS in production
```

---

## License

This project is developed as part of an internship at **GravatonWeb Technology**.
