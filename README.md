# PFTB — Personal Finance Tracker Backend

Backend service for a personal finance tracker application, built with **Spring Boot 3**. It exposes REST APIs for **authentication** and **transaction management**, persists data using **Spring Data JPA** with **PostgreSQL**, and uses **JWT** for securing endpoints.

## Tech stack

- Java **17**
- Spring Boot **3.3.0**
  - Spring Web
  - Spring Security
  - Spring Data JPA
  - Validation
- PostgreSQL (runtime)
- JWT via `io.jsonwebtoken` (JJWT)
- Lombok
- Bucket4j (rate limiting)
- Maven (includes `mvnw` / `mvnw.cmd`)

## Project structure (high level)

- `src/main/java/com/example/pftb`
  - `PftbApplication.java` — Spring Boot entry point
  - `controller/`
    - `AuthController.java` — auth endpoints
    - `TransactionController.java` — transaction endpoints
    - `GlobalExceptionHandler.java` — centralized error handling
  - `config/` — security/app configuration (JWT, Spring Security, etc.)
  - `entity/` — JPA entities
  - `repository/` — Spring Data repositories
  - `service/` — business logic/services
  - `dto/` — request/response DTOs
- `src/main/resources/application.properties` — app configuration (DB + JWT)

## Prerequisites

- Java 17+
- PostgreSQL running locally or reachable from the app
- (Optional) Maven installed — or use the included Maven wrapper (`./mvnw`)

## Configuration

Configuration is defined in `src/main/resources/application.properties` and is driven by environment variables with defaults:

### Database

- `DB_URL` (default: `jdbc:postgresql://localhost:5432/Personal_Finance_DB`)
- `DB_USERNAME` (default: `postgres`)
- `DB_PASSWORD` (default: `Auca`)

### JWT

- `JWT_SECRET` (default: a hex-like string in `application.properties`)
- `JWT_EXPIRATION` (default: `3600000` ms = 1 hour)

> Note: For HS256, the JWT secret must be at least 256 bits.

## Running the application

### 1) Start PostgreSQL and create a database

Create a database named `Personal_Finance_DB` (or set `DB_URL` to a database you already have).

### 2) Run the backend

Using Maven wrapper:

```bash
./mvnw spring-boot:run
```

Or build a jar and run:

```bash
./mvnw clean package
java -jar target/pftb-0.0.1-SNAPSHOT.jar
```

## API overview

Controllers present in the codebase:

- **AuthController**: authentication-related endpoints (e.g., login/register, JWT issuance).
- **TransactionController**: transaction CRUD and/or listing endpoints.
- **GlobalExceptionHandler**: consistent error responses across the API.

Because this README is generated from repository structure and dependencies, see the controller classes for the exact routes and payloads:
- `src/main/java/com/example/pftb/controller/AuthController.java`
- `src/main/java/com/example/pftb/controller/TransactionController.java`

## Persistence

- Uses **Spring Data JPA**
- `spring.jpa.hibernate.ddl-auto=update` is enabled by default (auto-updates schema on startup)

## Security

- Uses **Spring Security**
- JWT-based auth (via JJWT)

## Development notes

- The application name is configured as `spring.application.name=pftb`.
- SQL logging is disabled by default (`spring.jpa.show-sql=false`).

## License

Add your preferred license here (e.g., MIT). If this is private/internal, you can remove this section.
