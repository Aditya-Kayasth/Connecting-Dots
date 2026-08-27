# Core Business Microservice (`core-service`)

The `core-service` microservice is the central transactional engine of the Connecting Dots platform. It handles user authentication, profile creation, problem statement management, project applications, messaging threads, and review ratings while maintaining relational data persistence in Neon PostgreSQL.

---

## 1. Core Service Overview

- **Port:** `8081`
- **Container Name:** `core-service`
- **Primary Role:** User authentication, domain entity persistence, transactional workflow enforcement, and integration with Upstash QStash and Cloudinary.

---

## 2. Tech Stack & Dependencies

- **Language & JDK:** Java 25
- **Framework:** Spring Boot 4.0.7 & Spring Security
- **Data Access:** Spring Data JPA / Hibernate
- **Database:** Neon Serverless PostgreSQL
- **Migrations:** Flyway (`db/migration/V1__...` to `V8__...`)
- **Security & Tokens:** JJWT (`io.jsonwebtoken` 0.12.x) & BCrypt Password Encoder
- **File Uploads:** Cloudinary Java SDK
- **Testing:** Testcontainers (PostgreSQL Docker container), MockMvc, Spring Security Test

---

## 3. Endpoints & API Matrix

| HTTP Method | Endpoint Path | Access Level | Description / Payload |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/core/auth/register` | `permitAll()` | Registers user & issues JWT token. |
| `POST` | `/api/v1/core/auth/login` | `permitAll()` | Validates credentials & issues JWT token. |
| `GET` | `/api/v1/core/problem-statements` | `permitAll()` | Public guest discovery of open problem statements (paged/filtered). |
| `POST` | `/api/v1/core/problem-statements` | `NGO` Role | Creates problem statement & triggers QStash AI ingestion. |
| `GET` | `/api/v1/core/profiles/ngos` | `permitAll()` | Public directory of NGO profiles. |
| `GET` | `/api/v1/core/profiles/contributors` | `permitAll()` | Public directory of Contributor profiles. |
| `POST` | `/api/v1/core/profiles/ngo` | `NGO` Role | Creates NGO profile for authenticated user. |
| `POST` | `/api/v1/core/profiles/contributor` | `CONTRIBUTOR` Role | Creates Contributor profile for authenticated user. |
| `POST` | `/api/v1/core/applications` | `CONTRIBUTOR` Role | Submits project application for a problem. |
| `POST` | `/api/v1/core/applications/{id}/messages` | Application Participant | Sends thread-isolated chat message. |
| `POST` | `/api/v1/core/reviews` | Authenticated | Submits project completion review. |

---

## 4. Key Working Logic & Features

### A. Security & Guest Discovery Model
- **Public Guest Access**: All `GET` endpoints for discovery (`/problem-statements/**`, `/profiles/**`, `/reviews/**`) are marked `permitAll()` in `SecurityConfig`. Anonymous guests can explore records freely.
- **Protected Mutations**: Write actions (`POST`, `PUT`, `DELETE`) require authentication. Unauthenticated requests trigger `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)`, returning clean `401 Unauthorized` responses.
- **JWT Role Extraction**: `JwtAuthenticationFilter` extracts `"role"` claims from incoming tokens and injects `SimpleGrantedAuthority("ROLE_" + role)` into `SecurityContextHolder`.

### B. Application-Isolated Messaging
Messages are tied to a specific `application_id` (`application_messages` table). When an NGO assigns 2 different problems to 2 separate contributors:
- **Application 1** (`Problem A` + `Contributor 1`) has isolated thread `app-uuid-1`.
- **Application 2** (`Problem B` + `Contributor 2`) has isolated thread `app-uuid-2`.
- `MessageService` verifies that the sender is either the Contributor or NGO tied to that specific application ID, enforcing privacy between threads.

### C. Database Versioning (Flyway)
Flyway automatically executes migration scripts on startup:
- `V1__init_schema.sql` (Users, Profiles, Problems)
- `V2__add_applications.sql`
- `V3__add_messages.sql`
- `V4` - `V8` (Indexes, Reviews, Schema Enhancements)

---

## 5. Key Annotations & Concepts

- **`@RestController` & `@RequestMapping`**: Declares Spring MVC REST controller with base URI path.
- **`@Valid` & `@NotBlank`**: Enforces DTO validation on incoming JSON payloads.
- **`@Transactional`**: Wraps database modifications in ACID database transactions.
- **`@Entity` & `@Table`**: Maps Java classes (`User`, `ProblemStatement`, `Message`) to PostgreSQL tables.
- **`@CreationTimestamp`**: Hibernate annotation auto-populating `created_at` timestamp fields.
- **`@Value("${jwt.secret:${JWT_SECRET}}")`**: Injects configuration variables with fallback defaults.

---

## 6. Key Engineering Challenges & Architectural Decisions

Core Service security architecture, guest vs mutation controls, application chat thread isolation, and Flyway database migration decisions are documented in the master guide:
👉 **[ARCHITECTURE_DECISIONS_AND_SOLUTIONS.md](../../ARCHITECTURE_DECISIONS_AND_SOLUTIONS.md)**
