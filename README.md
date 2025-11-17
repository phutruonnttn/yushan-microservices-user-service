# Yushan User Service

> 👤 **User Service for Yushan Platform (Phase 2 - Microservices)** - Manages user accounts, profiles, authentication, and user-related operations for the gamified web novel reading platform.

## 📋 Overview

User Service is one of the main microservices of Yushan Platform (Phase 2), responsible for managing users, authentication, and user-related operations. This service uses JWT for authentication, Kafka to publish events, and Redis for caching.

## 🚀 Tech Stack

- **Framework**: Spring Boot 3.4.10
- **Language**: Java 21
- **Spring Cloud**: 2024.0.2
- **Database**: PostgreSQL 15+ (MyBatis)
- **Cache**: Redis
- **Message Queue**: Apache Kafka
- **Build Tool**: Maven
- **Service Discovery**: Eureka Client
- **Config**: Spring Cloud Config Client

## Architecture Overview

```
┌─────────────────────────────┐
│   Eureka Service Registry   │
│       localhost:8761        │
└──────────────┬──────────────┘
               │
┌──────────────┴──────────────┐
│   Service Registration &     │
│      Discovery Layer         │
└──────────────┬──────────────┘
               │
    ┌──────────┴──────────┬───────────────┬──────────┬──────────┐
    │                     │               │          │          │
    ▼                     ▼               ▼          ▼          ▼
┌────────┐          ┌─────────┐  ┌────────────┐ ┌──────────┐ ┌──────────┐
│  User  │          │ Content │  │ Engagement │ │Gamifica- │ │Analytics │
│Service │◄────────►│ Service │◄─┤  Service   │ │  tion    │ │ Service  │
│ :8081  │          │  :8082  │  │   :8084    │ │ Service  │ │  :8083   │
└───┬────┘          └─────────┘  └────────────┘ │  :8085   │ └──────────┘
    │                     │              │       └──────────┘      │
    └─────────────────────┴──────────────┴───────────────────────┘
    │                 Inter-service Communication
    │                   (via Feign Clients)
    ▼
┌───────────────┐
│  Auth & User  │
│  Management   │
│   JWT/OAuth   │
└───────────────┘
```

---
## 🚦 Getting Started

### Prerequisites

Before setting up User Service, ensure you have:
1. **Java 21** installed
2. **Maven 3.8+** or use the included Maven wrapper
3. **Eureka Service Registry** running
4. **Config Server** running
5. **PostgreSQL 15+** (for user data storage)
6. **Redis** (for session management and caching)
7. **Kafka** (for event publishing)

---
### Quick Setup

**IMPORTANT**: Eureka Service Registry and Config Server must be running before starting any microservice.

```bash
# Clone repository
git clone https://github.com/phutruonnttn/yushan-microservices-user-service.git
cd yushan-microservices-user-service

# Option 1: Run with Docker (Recommended)
docker-compose up -d

# Option 2: Run locally (requires PostgreSQL, Redis, Kafka to be running)
./mvnw spring-boot:run
```

### Verify Eureka is Running

- Open: http://localhost:8761
- You should see the Eureka dashboard

---

## Expected Output

### Console Logs (Success)

```
2024-10-16 10:30:15 - Starting UserServiceApplication
2024-10-16 10:30:18 - Tomcat started on port(s): 8081 (http)
2024-10-16 10:30:20 - DiscoveryClient_USER-SERVICE/user-service:8081 - registration status: 204
2024-10-16 10:30:20 - Started UserServiceApplication in 8.5 seconds
```

### Eureka Dashboard

```
Instances currently registered with Eureka:
✅ USER-SERVICE - 1 instance(s)
   Instance ID: user-service:8081
   Status: UP (1)
```

---

## 📡 API Endpoints

### Health Check
- `GET /api/v1/health` - Service health status

### Authentication
- `POST /api/v1/auth/register` - Register new user account (includes email verification)
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/logout` - User logout
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/auth/send-email` - Send email verification code

### User Management
- `GET /api/v1/users/me` - Get current user info
- `GET /api/v1/users/{userId}` - Get user info by ID
- `PUT /api/v1/users/{id}/profile` - Update profile
- `POST /api/v1/users/send-email-change-verification` - Send email change verification code
- `POST /api/v1/users/batch/get` - Get multiple users (batch) - **POST method**
- `GET /api/v1/users/all/ranking` - Get all users for ranking

### Library Management
- `GET /api/v1/library` - Get personal library (with pagination)
- `POST /api/v1/library/{novelId}` - Add novel to library
- `DELETE /api/v1/library/{novelId}` - Remove from library
- `DELETE /api/v1/library/batch` - Batch remove novels from library
- `GET /api/v1/library/check/{novelId}` - Check if novel is in library
- `GET /api/v1/library/{novelId}` - Get novel info (progress) in library
- `PATCH /api/v1/library/{novelId}/progress` - Update reading progress

### Admin Endpoints
- `GET /api/v1/admin/users` - List users (paginated, filtered)
- `POST /api/v1/admin/promote-to-admin` - Promote to Admin
- `PUT /api/v1/admin/users/{uuid}/status` - Update user status

### Author Endpoints
- `POST /api/v1/author/send-email-author-verification` - Send author verification email
- `POST /api/v1/author/upgrade-to-author` - Upgrade to author

---

## ✨ Key Features

### 🔐 Authentication & Authorization
- JWT-based authentication with refresh tokens
- Email verification with OTP (Redis)
- Password hashing with BCrypt
- Role-based access control (RBAC): READER, AUTHOR, ADMIN
- Session management with Redis

### 👤 User Profile Management
- Comprehensive user profile management
- Avatar and personal information
- User statistics
- Batch lookup users

### 📚 Library Management
- Personal library
- Reading progress tracking
- Integration with Content Service via Feign

### 📧 Email Service
- Send OTP code via email
- Email verification
- Author verification

### 🎯 Event Publishing
- Kafka events: `UserRegisteredEvent`, `UserLoggedInEvent`
- Topics: `user.events`, `active`
- Integration with Gamification Service

### 🔍 Inter-service Communication
- Feign Client: `ContentServiceClient` (validate novel/chapter)
- JWT token forwarding in inter-service calls

---

## 🏗️ Project Structure

```
com.yushan.user_service/
├── controller/          # REST API endpoints
│   ├── AuthController.java
│   ├── UserController.java
│   ├── LibraryController.java
│   ├── AdminController.java
│   └── AuthorController.java
├── service/            # Business logic
│   ├── AuthService.java
│   ├── UserService.java
│   ├── LibraryService.java
│   ├── AdminService.java
│   └── MailService.java
├── dao/               # MyBatis mappers
│   ├── UserMapper.java
│   └── LibraryMapper.java
├── entity/            # Database entities
│   ├── User.java
│   └── Library.java
├── dto/               # Data Transfer Objects
├── config/            # Configuration
│   ├── SecurityConfig.java
│   ├── KafkaProducerConfig.java
│   └── DatabaseConfig.java
├── security/          # Security components
│   ├── JwtAuthenticationFilter.java
│   └── CustomUserDetailsService.java
└── event/             # Kafka producers
    ├── UserEventProducer.java
    └── UserActivityEventProducer.java
```

## 💾 Database Schema

- **users** - User account information
- **library** - Personal library
- **novel_library** - Novel-library mapping

---

## 🔗 Inter-service Communication

User Service communicates with:
- **Content Service**: Validate novel/chapter when adding to library
- **Gamification Service**: Receives events from Kafka to process rewards

## 📊 Monitoring

User Service exposes metrics through:
- Spring Boot Actuator endpoints (`/actuator/health`, `/actuator/metrics`)
- Custom metrics: login success/failure rates, registration counts

## 🐛 Troubleshooting

**Problem: Service won't register with Eureka**
- Ensure Eureka is running: `docker ps`
- Check logs: Look for "DiscoveryClient" messages
- Verify defaultZone URL is correct

**Problem: Port 8081 already in use**
- Find process: `lsof -i :8081` (Mac/Linux) or `netstat -ano | findstr :8081` (Windows)
- Kill process or change port in application.yml

**Problem: Database connection fails**
- Verify PostgreSQL is running: `docker ps | grep yushan-postgres`
- Check database credentials in application.yml
- Test connection: `psql -h localhost -U yushan_user -d yushan_user`

**Problem: Redis connection fails**
- Verify Redis is running: `docker ps | grep redis`
- Check Redis connection: `redis-cli ping`
- Verify Redis host and port in application.yml

**Problem: Build fails**
- Ensure Java 21 is installed: `java -version`
- Check Maven: `./mvnw -version`
- Clean and rebuild: `./mvnw clean install -U`

**Problem: JWT token validation fails**
- Check JWT_SECRET environment variable
- Verify token expiration settings
- Check system clock synchronization
- Review token format in requests

**Problem: Email verification not working**
- Check email service configuration
- Verify SMTP settings
- Review email template configuration
- Check spam folder

---

## Performance Tips
1. **Session Caching**: Use Redis for session storage
2. **Profile Caching**: Cache frequently accessed profiles
3. **Rate Limiting**: Implement rate limits on auth endpoints
4. **Database Indexing**: Index username, email, and user_id columns
5. **Connection Pooling**: Configure appropriate connection pool sizes

---

## Security Best Practices
1. **Password Policy**: Enforce strong password requirements
2. **Rate Limiting**: Prevent brute force attacks
3. **Account Lockout**: Lock accounts after failed attempts
4. **Audit Logging**: Log all authentication events
5. **Token Rotation**: Rotate JWT tokens regularly
6. **HTTPS Only**: Force HTTPS in production
7. **CORS Configuration**: Properly configure CORS policies

---

## Inter-Service Communication
The User Service communicates with:
- **Engagement Service**: User profile data for comments/reviews
- **Gamification Service**: User achievements and levels
- **Analytics Service**: User behavior tracking
- **Content Service**: Author verification and permissions

---

## Monitoring
The User Service exposes metrics through:
- Spring Boot Actuator endpoints (`/actuator/metrics`)
- Custom authentication metrics (login success/failure rates)
- Session count and activity
- Failed login attempts
- API response times

---

## 🛠️ Built With

- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [Spring Cloud](https://spring.io/projects/spring-cloud) - Microservices framework
- [MyBatis](https://mybatis.org/) - SQL mapping framework
- [PostgreSQL](https://www.postgresql.org/) - Database
- [Redis](https://redis.io/) - Caching và session storage
- [Apache Kafka](https://kafka.apache.org/) - Message queue
- [Spring Security](https://spring.io/projects/spring-security) - Security framework

## 📄 License

This project is part of the Yushan Platform ecosystem.

## 🔗 Links

- **API Gateway**: [yushan-microservices-api-gateway](https://github.com/phutruonnttn/yushan-microservices-api-gateway)
- **Service Registry**: [yushan-microservices-service-registry](https://github.com/phutruonnttn/yushan-microservices-service-registry)
- **Config Server**: [yushan-microservices-config-server](https://github.com/phutruonnttn/yushan-microservices-config-server)
- **Platform Documentation**: [yushan-platform-docs](https://github.com/phutruonnttn/yushan-platform-docs) - Complete documentation for all phases
- **Phase 2 Architecture**: See [Phase 2 Microservices Architecture](https://github.com/phutruonnttn/yushan-platform-docs/blob/main/docs/phase2-microservices/PHASE2_MICROSERVICES_ARCHITECTURE.md)

---

**Yushan User Service** - User management and authentication for microservices 👤
