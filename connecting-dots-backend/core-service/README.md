# Core Service (`core-service`)

## Role & Responsibilities
The primary business logic engine handling user authentication, profile management, problem ingestion orchestration, contributor matching applications, messaging threads, and trust reputation reviews.

## Tech Stack
* Spring Boot 4.0.7 / Spring Data JPA & Hibernate
* Spring Cloud 2025.1.2
* Spring Security & JWT (Stateless Authentication)
* Flyway Database Migration (`V1` to `V8`)
* Neon Serverless PostgreSQL
* Testcontainers & WireMock (Integration Testing)
* Upstash QStash Client
* Java 25

## Key Architecture Patterns
* **Optimistic Concurrency Control:** Implements `@Version` annotations on domain models to prevent conflicting concurrent edits.
* **Event-Driven Stat Recomputation:** Automatically updates contributor `completedProjects` counters upon NGO project sign-off.
* **Stateless Security Model:** Enforces JWT verification per request via custom `JwtAuthenticationFilter` integrated with Spring Security context.

## Port & Integration Points
* **Default Port:** `8081`
* **Database:** Neon PostgreSQL (JDBC)
* **Async Ingestion Queue:** Upstash QStash HTTP Publisher

## Key Functional Modules & Endpoints
* **Auth:** `POST /api/v1/core/auth/register`, `POST /api/v1/core/auth/login`
* **Profiles:** `POST /api/v1/core/profiles/ngo`, `POST /api/v1/core/profiles/contributor`
* **Problems:** `POST /api/v1/core/problem-statements`, `GET /api/v1/core/problem-statements`, `POST /.../ingest`
* **Applications:** `POST /api/v1/core/applications`, `PUT /.../status`, `PUT /.../complete`
* **Messaging:** `POST /api/v1/core/applications/{id}/messages`, `GET /.../messages`
* **Reviews:** `POST /api/v1/core/reviews`, `GET /api/v1/core/reviews/user/{id}`
* **File Upload:** `GET /api/v1/core/files/signature`
